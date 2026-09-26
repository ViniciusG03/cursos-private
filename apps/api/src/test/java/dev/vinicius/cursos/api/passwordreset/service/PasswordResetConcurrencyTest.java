package dev.vinicius.cursos.api.passwordreset.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.support.AccessIntegrationTest;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Corridas da fila de recuperação com ordem de execução forçada: o worker real (agendador em segundo
 * plano) é segurado no meio do envio, e o teste só libera depois de confirmar no {@code pg_stat_activity}
 * que a outra operação está de fato esperando um lock.
 */
class PasswordResetConcurrencyTest extends AccessIntegrationTest {

	private static final String NEW_PASSWORD = "senha nova depois da corrida";

	@Autowired
	private PasswordResetRequestService resetRequestService;

	@Autowired
	private PasswordResetConfirmation resetConfirmation;

	@Autowired
	private PasswordResetDispatcher dispatcher;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void createMemberAccount() {
		createMember("ana@x.com");
	}

	@Test
	void failedAttemptIsRecordedBeforeAnyOtherWorkerCanTakeTheRequest() throws Exception {
		mailbox.simulateOutage(true);
		mailbox.holdDeliveries();
		UUID requestId = resetRequestService.requestReset(AccountEmail.parse("ana@x.com"));
		mailbox.awaitDeliveryHeld();

		// "Outro worker": trava a mesma linha sem SKIP LOCKED e anota o que vê assim que consegue o lock.
		CompletableFuture<Map<String, Object>> otherWorkerView = CompletableFuture
			.supplyAsync(() -> transactionTemplate.execute(tx -> jdbcTemplate.queryForMap("""
					SELECT status, attempt_count, next_attempt_at FROM password_reset_requests
					WHERE id = ? FOR UPDATE""", requestId)));
		awaitSessionWaitingOnLock();
		mailbox.releaseHeldDeliveries();

		Map<String, Object> seenByOtherWorker = otherWorkerView.get(10, TimeUnit.SECONDS);
		assertThat(seenByOtherWorker).containsEntry("status", "PENDING")
			.containsEntry("attempt_count", 1)
			.containsEntry("next_attempt_at", Timestamp.from(clock.instant().plus(Duration.ofMinutes(1))));
		assertThat(dispatcher.dispatchDue()).isZero();
		assertThat(mailbox.deliveredMails()).isEmpty();
		assertThat(countUsableTokens()).isZero();
	}

	@Test
	void confirmingALinkWhileANewRequestIsProcessedDoesNotDeadlock() throws Exception {
		resetRequestService.requestReset(AccountEmail.parse("ana@x.com"));
		String firstToken = awaitResetLinkTo("ana@x.com", 1);
		CountDownLatch confirmedButNotCommitted = new CountDownLatch(1);
		CountDownLatch releaseCommit = new CountDownLatch(1);

		CompletableFuture<Void> confirmation = CompletableFuture.runAsync(() -> transactionTemplate
			.executeWithoutResult(tx -> {
				resetConfirmation.confirm(firstToken, NEW_PASSWORD);
				confirmedButNotCommitted.countDown();
				awaitOrFail(releaseCommit);
			}));
		awaitOrFail(confirmedButNotCommitted);
		resetRequestService.requestReset(AccountEmail.parse("ana@x.com"));
		awaitSessionWaitingOnLock();
		releaseCommit.countDown();

		confirmation.get(15, TimeUnit.SECONDS);
		String secondToken = awaitResetLinkTo("ana@x.com", 2);
		assertThat(passwordEncoder.matches(NEW_PASSWORD, accountRepository.findByEmail("ana@x.com").orElseThrow()
			.getPasswordHash())).isTrue();
		assertThat(jdbcTemplate.queryForList("SELECT status FROM password_reset_requests", String.class))
			.containsOnly("SENT");
		resetConfirmation.confirm(secondToken, "outra senha nova e comprida");
	}

	@Test
	void unexpectedFailureAfterRollbackIsRecordedOnlyOnceAndLeavesNoToken() throws Exception {
		mailbox.simulateCrash(true);
		resetRequestService.requestReset(AccountEmail.parse("ana@x.com"));
		awaitDueResetRequestsHandled();

		assertThat(readRequest()).containsEntry("status", "PENDING").containsEntry("attempt_count", 1)
			.containsEntry("last_error", "IllegalStateException");
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM password_reset_tokens", Integer.class)).isZero();
	}

	@Test
	void staleUnexpectedFailureDoesNotOverwriteAnotherWorkersSuccess() {
		UUID requestId = resetRequestService.requestReset(AccountEmail.parse("ana@x.com"));
		awaitResetLinkTo("ana@x.com", 1);

		// Ordem forçada: este worker pegou o pedido com 0 tentativas, falhou, e outro já o enviou antes do registro.
		transactionTemplate.executeWithoutResult(tx -> dispatcher
			.recordUnexpectedFailure(new PasswordResetDispatcher.ClaimedRequest(requestId, 0), "IllegalStateException"));

		assertThat(readRequest()).containsEntry("status", "SENT").containsEntry("attempt_count", 0);
	}

	@Test
	void staleUnexpectedFailureDoesNotCountTwiceAfterAnotherWorkersFailure() {
		mailbox.simulateOutage(true);
		UUID requestId = resetRequestService.requestReset(AccountEmail.parse("ana@x.com"));
		awaitDueResetRequestsHandled();

		transactionTemplate.executeWithoutResult(tx -> dispatcher
			.recordUnexpectedFailure(new PasswordResetDispatcher.ClaimedRequest(requestId, 0), "IllegalStateException"));

		assertThat(readRequest()).containsEntry("attempt_count", 1);
		assertThat((String) readRequest().get("last_error")).contains("simulated outage");
	}

	private Map<String, Object> readRequest() {
		return jdbcTemplate.queryForMap("SELECT status, attempt_count, last_error FROM password_reset_requests");
	}

	private int countUsableTokens() {
		return jdbcTemplate.queryForObject(
				"SELECT count(*) FROM password_reset_tokens WHERE consumed_at IS NULL AND revoked_at IS NULL",
				Integer.class);
	}

	private static void awaitOrFail(CountDownLatch latch) {
		try {
			if (!latch.await(10, TimeUnit.SECONDS)) {
				throw new AssertionError("expected latch to open within 10s, got timeout");
			}
		}
		catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new AssertionError("interrupted while waiting for latch", interrupted);
		}
	}

}
