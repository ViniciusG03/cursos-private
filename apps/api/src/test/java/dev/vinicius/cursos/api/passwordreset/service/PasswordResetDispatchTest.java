package dev.vinicius.cursos.api.passwordreset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.support.AccessIntegrationTest;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

/**
 * Fila durável da recuperação com o worker real: o agendador do Spring roda em segundo plano durante os
 * testes (varredura de 100 ms, 3 tentativas no perfil de teste), sem executor síncrono de mentira.
 */
@ExtendWith(OutputCaptureExtension.class)
class PasswordResetDispatchTest extends AccessIntegrationTest {

	@Autowired
	private PasswordResetRequestService resetRequestService;

	@Autowired
	private PasswordResetConfirmation resetConfirmation;

	@Autowired
	private PasswordResetDispatcher dispatcher;

	@BeforeEach
	void createMemberAccount() {
		createMember("ana@x.com");
	}

	@Test
	void acceptedRequestIsDeliveredLaterByTheSchedulerThread() throws Exception {
		mockMvc
			.perform(post("/api/auth/password-resets/request").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ana@x.com\"}"))
			.andExpect(status().isAccepted());

		String rawToken = awaitResetLinkTo("ana@x.com", 1);

		assertThat(mailbox.deliveringThreads()).singleElement().isNotEqualTo(Thread.currentThread().getName());
		assertThat(readRequestColumns()).containsEntry("status", "SENT").containsEntry("attempt_count", 0);
		resetConfirmation.confirm(rawToken, "senha nova e bem comprida");
	}

	@Test
	void requestLeftPendingByAStoppedProcessIsDeliveredAfterRestart() {
		// Linha como o endpoint deixa ao responder 202; o processo "parou" antes de qualquer envio.
		jdbcTemplate.update("""
				INSERT INTO password_reset_requests (email, requested_at, status, next_attempt_at)
				VALUES ('ana@x.com', ?, 'PENDING', ?)""", Timestamp.from(clock.instant()), Timestamp.from(clock.instant()));

		String rawToken = awaitResetLinkTo("ana@x.com", 1);

		resetConfirmation.confirm(rawToken, "senha nova e bem comprida");
	}

	@Test
	void requestIsPersistedBeforeTheEndpointAnswers() {
		mailbox.simulateOutage(true);

		resetRequestService.requestReset(AccountEmail.parse("ana@x.com"));

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM password_reset_requests WHERE email = 'ana@x.com'",
				Integer.class))
			.isEqualTo(1);
	}

	@Test
	void failedDeliveryIsRecordedLeavesNoUsableTokenAndIsRetriedWithBackoff() throws Exception {
		mailbox.simulateOutage(true);
		resetRequestService.requestReset(AccountEmail.parse("ana@x.com"));
		awaitAttemptCount(1);

		assertThat(readRequestColumns()).containsEntry("status", "PENDING")
			.containsEntry("next_attempt_at", Timestamp.from(clock.instant().plus(Duration.ofMinutes(1))));
		assertThat((String) readRequestColumns().get("last_error")).contains("simulated outage");
		// O token do link que não saiu fica revogado (só o hash, na mesma transação que registrou a falha).
		assertThat(jdbcTemplate.queryForObject(
				"SELECT count(*) FROM password_reset_tokens WHERE consumed_at IS NULL AND revoked_at IS NULL",
				Integer.class))
			.isZero();

		mailbox.simulateOutage(false);
		clock.advance(Duration.ofMinutes(1));
		String rawToken = awaitResetLinkTo("ana@x.com", 1);

		assertThat(readRequestColumns()).containsEntry("status", "SENT").containsEntry("attempt_count", 1);
		resetConfirmation.confirm(rawToken, "senha nova e bem comprida");
	}

	@Test
	void givesUpAfterMaxAttemptsVisiblyAndANewRequestStillWorks(CapturedOutput output) throws Exception {
		mailbox.simulateOutage(true);
		resetRequestService.requestReset(AccountEmail.parse("ana@x.com"));
		awaitAttemptCount(1);
		clock.advance(Duration.ofMinutes(1));
		awaitAttemptCount(2);
		clock.advance(Duration.ofMinutes(2));
		awaitAttemptCount(3);

		assertThat(readRequestColumns()).containsEntry("status", "FAILED").containsKey("completed_at");
		assertThat(output.getAll()).contains("password reset delivery attempt failed").contains("\"status\":\"FAILED\"");

		mailbox.simulateOutage(false);
		resetRequestService.requestReset(AccountEmail.parse("ana@x.com"));
		resetConfirmation.confirm(awaitResetLinkTo("ana@x.com", 1), "senha nova e bem comprida");
	}

	@Test
	void concurrentWorkersDeliverEachRequestOnceWithoutSpuriousFailures() throws Exception {
		IntStream.range(0, 5).forEach(i -> insertPendingRequest("ana@x.com"));
		CountDownLatch start = new CountDownLatch(1);

		List<CompletableFuture<Integer>> workers = IntStream.range(0, 4).mapToObj(i -> CompletableFuture.supplyAsync(() -> {
			awaitQuietly(start);
			return dispatcher.dispatchDue();
		})).toList();
		start.countDown();
		CompletableFuture.allOf(workers.toArray(CompletableFuture[]::new)).join();
		awaitDueResetRequestsHandled();

		assertThat(mailbox.deliveredMails()).hasSize(5);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT count(*) FROM password_reset_requests WHERE status = 'SENT' AND attempt_count = 0", Integer.class))
			.isEqualTo(5);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT count(*) FROM password_reset_tokens WHERE consumed_at IS NULL AND revoked_at IS NULL",
				Integer.class))
			.isEqualTo(1);
	}

	@Test
	void neverStoresOrLogsTheRawToken(CapturedOutput output) {
		resetRequestService.requestReset(AccountEmail.parse("ana@x.com"));
		String rawToken = awaitResetLinkTo("ana@x.com", 1);

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM password_reset_requests r WHERE r::text LIKE ?",
				Integer.class, "%" + rawToken + "%"))
			.isZero();
		assertThat(output.getAll()).contains("password reset request completed").doesNotContain(rawToken);
	}

	@Test
	void purgeRemovesOnlyRequestsCompletedBeforeRetention() throws Exception {
		resetRequestService.requestReset(AccountEmail.parse("ninguem@x.com"));
		awaitDueResetRequestsHandled();
		mailbox.simulateOutage(true);
		clock.advance(Duration.ofDays(31));
		resetRequestService.requestReset(AccountEmail.parse("ana@x.com"));

		assertThat(dispatcher.purgeCompleted()).isEqualTo(1);
		assertThat(jdbcTemplate.queryForList("SELECT email FROM password_reset_requests", String.class))
			.containsExactly("ana@x.com");
	}

	private void insertPendingRequest(String email) {
		jdbcTemplate.update("""
				INSERT INTO password_reset_requests (email, requested_at, status, next_attempt_at)
				VALUES (?, ?, 'PENDING', ?)""", email, Timestamp.from(clock.instant()), Timestamp.from(clock.instant()));
	}

	private void awaitAttemptCount(int expectedAttempts) throws InterruptedException {
		java.time.Instant deadline = java.time.Instant.now().plusSeconds(10);
		while (((Number) readRequestColumns().get("attempt_count")).intValue() < expectedAttempts) {
			if (java.time.Instant.now().isAfter(deadline)) {
				throw new AssertionError("expected %d failed attempts within 10s, got %s".formatted(expectedAttempts,
						readRequestColumns()));
			}
			Thread.sleep(20);
		}
	}

	private Map<String, Object> readRequestColumns() {
		return jdbcTemplate.queryForMap("""
				SELECT status, attempt_count, next_attempt_at, last_error, completed_at
				FROM password_reset_requests WHERE email = 'ana@x.com' ORDER BY requested_at DESC LIMIT 1""");
	}

	private static void awaitQuietly(CountDownLatch latch) {
		try {
			latch.await();
		}
		catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("interrupted before starting worker", interrupted);
		}
	}

}
