package dev.vinicius.cursos.api.invitation.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vinicius.cursos.api.support.AccessIntegrationTest;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Aceite e reenvio simultâneos para o mesmo e-mail. A ordem reproduzida é a da revisão do marco 2: o
 * aceite já consumiu o convite e criou a conta, mas ainda não confirmou; o reenvio começa nesse
 * intervalo. Sem coordenação por e-mail, o reenvio via "sem conta" e emitia um convite novo.
 */
class InvitationAcceptResendRaceTest extends AccessIntegrationTest {

	private static final String CHOSEN_PASSWORD = "senha escolhida pela ana";

	@Autowired
	private InvitationService invitationService;

	@Autowired
	private InvitationAcceptance invitationAcceptance;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private UUID adminId;

	@BeforeEach
	void createInvitingAdmin() {
		adminId = createAdmin().getId();
	}

	@Test
	void resendStartedWhileAcceptanceIsCommittingSeesTheNewAccount() throws Exception {
		invitationService.issue("ana@x.com", adminId);
		String rawToken = mailbox.lastTokenSentTo("ana@x.com");
		CountDownLatch acceptedButNotCommitted = new CountDownLatch(1);
		CountDownLatch releaseCommit = new CountDownLatch(1);

		CompletableFuture<Void> acceptance = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(tx -> {
			invitationAcceptance.accept(rawToken, CHOSEN_PASSWORD);
			acceptedButNotCommitted.countDown();
			awaitOrFail(releaseCommit);
		}));
		awaitOrFail(acceptedButNotCommitted);
		CompletableFuture<InvitationView> resend = CompletableFuture
			.supplyAsync(() -> invitationService.issue("ana@x.com", adminId));
		awaitSessionWaitingOnLock();
		releaseCommit.countDown();
		acceptance.get(10, TimeUnit.SECONDS);

		assertThat(outcomeOf(resend)).isInstanceOf(InvitationConflictException.class);
		assertThat(invitationService.listOpen()).isEmpty();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE email = 'ana@x.com'", Integer.class))
			.isEqualTo(1);
		assertThat(mailbox.deliveredMails()).hasSize(1);
	}

	@Test
	void acceptanceStartedWhileResendIsCommittingSeesTheRevokedInvitation() throws Exception {
		invitationService.issue("ana@x.com", adminId);
		String firstToken = mailbox.lastTokenSentTo("ana@x.com");
		CountDownLatch resentButNotCommitted = new CountDownLatch(1);
		CountDownLatch releaseCommit = new CountDownLatch(1);

		CompletableFuture<Void> resend = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(tx -> {
			invitationService.issue("ana@x.com", adminId);
			resentButNotCommitted.countDown();
			awaitOrFail(releaseCommit);
		}));
		awaitOrFail(resentButNotCommitted);
		CompletableFuture<UUID> acceptance = CompletableFuture
			.supplyAsync(() -> invitationAcceptance.accept(firstToken, CHOSEN_PASSWORD));
		awaitSessionWaitingOnLock();
		releaseCommit.countDown();
		resend.get(10, TimeUnit.SECONDS);

		assertThat(outcomeOf(acceptance))
			.isInstanceOf(dev.vinicius.cursos.api.onetimetoken.InvalidOneTimeTokenException.class);
		assertThat(accountRepository.existsByEmail("ana@x.com")).isFalse();
		assertThat(invitationService.listOpen()).hasSize(1);
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

	private static Object outcomeOf(CompletableFuture<?> future) throws InterruptedException {
		try {
			return future.get(10, TimeUnit.SECONDS);
		}
		catch (ExecutionException failure) {
			return failure.getCause();
		}
		catch (java.util.concurrent.TimeoutException timeout) {
			throw new AssertionError("expected the concurrent operation to finish within 10s", timeout);
		}
	}

}
