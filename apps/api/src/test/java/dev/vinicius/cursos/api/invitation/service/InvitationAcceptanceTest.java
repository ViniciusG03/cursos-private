package dev.vinicius.cursos.api.invitation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vinicius.cursos.api.account.domain.UserAccount;
import dev.vinicius.cursos.api.account.domain.UserRole;
import dev.vinicius.cursos.api.account.service.PassphrasePolicyViolationException;
import dev.vinicius.cursos.api.invitation.domain.Invitation;
import dev.vinicius.cursos.api.onetimetoken.InvalidOneTimeTokenException;
import dev.vinicius.cursos.api.support.AccessIntegrationTest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class InvitationAcceptanceTest extends AccessIntegrationTest {

	private static final String CHOSEN_PASSWORD = "a senha que a ana escolheu";

	@Autowired
	private InvitationService invitationService;

	@Autowired
	private InvitationAcceptance invitationAcceptance;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private UUID adminId;

	@BeforeEach
	void createInvitingAdmin() {
		adminId = createAdmin().getId();
	}

	@Test
	void acceptanceCreatesMemberWithChosenPasswordHashAndConsumesInvitation() {
		String rawToken = inviteAndReadToken("ana@x.com");

		UUID memberId = invitationAcceptance.accept(rawToken, CHOSEN_PASSWORD);

		UserAccount member = accountRepository.findById(memberId).orElseThrow();
		assertThat(member.getRole()).isEqualTo(UserRole.MEMBER);
		assertThat(member.getEmail().value()).isEqualTo("ana@x.com");
		assertThat(member.getPasswordHash()).doesNotContain(CHOSEN_PASSWORD);
		assertThat(passwordEncoder.matches(CHOSEN_PASSWORD, member.getPasswordHash())).isTrue();
		assertThat(invitationService.listOpen()).isEmpty();
	}

	@Test
	void secondUseIsRejected() {
		String rawToken = inviteAndReadToken("ana@x.com");
		invitationAcceptance.accept(rawToken, CHOSEN_PASSWORD);

		assertThatThrownBy(() -> invitationAcceptance.accept(rawToken, "outra senha bem comprida"))
			.isInstanceOf(InvalidOneTimeTokenException.class);
		assertThat(accountRepository.count()).isEqualTo(2);
	}

	@Test
	void usableOneSecondBeforeExpiry() {
		String rawToken = inviteAndReadToken("ana@x.com");
		clock.advance(Invitation.VALIDITY.minusSeconds(1));

		assertThat(invitationAcceptance.accept(rawToken, CHOSEN_PASSWORD)).isNotNull();
	}

	@Test
	void expiredExactlyAt72Hours() {
		String rawToken = inviteAndReadToken("ana@x.com");
		clock.advance(Duration.ofHours(72));

		assertThatThrownBy(() -> invitationAcceptance.accept(rawToken, CHOSEN_PASSWORD))
			.isInstanceOf(InvalidOneTimeTokenException.class);
		assertThat(accountRepository.existsByEmail("ana@x.com")).isFalse();
	}

	@Test
	void unknownTokenIsRejectedWithGenericMessage() {
		assertThatThrownBy(() -> invitationAcceptance.accept("token-que-nao-existe", CHOSEN_PASSWORD))
			.isInstanceOf(InvalidOneTimeTokenException.class)
			.hasMessage("token is invalid or expired: expected an unused token from a recent e-mail link");
	}

	@Test
	void weakPasswordDoesNotConsumeInvitation() {
		String rawToken = inviteAndReadToken("ana@x.com");

		assertThatThrownBy(() -> invitationAcceptance.accept(rawToken, "curta"))
			.isInstanceOf(PassphrasePolicyViolationException.class);
		assertThat(invitationAcceptance.accept(rawToken, CHOSEN_PASSWORD)).isNotNull();
	}

	@Test
	void concurrentAcceptancesCreateExactlyOneAccount() throws Exception {
		String rawToken = inviteAndReadToken("ana@x.com");
		CountDownLatch startSignal = new CountDownLatch(1);
		Callable<Object> acceptance = () -> {
			startSignal.await();
			return invitationAcceptance.accept(rawToken, CHOSEN_PASSWORD);
		};

		List<Object> outcomes = runConcurrently(List.of(acceptance, acceptance, acceptance), startSignal);

		assertThat(outcomes).filteredOn(UUID.class::isInstance).hasSize(1);
		assertThat(outcomes).filteredOn(InvalidOneTimeTokenException.class::isInstance).hasSize(2);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE email = 'ana@x.com'", Integer.class))
			.isEqualTo(1);
	}

	private String inviteAndReadToken(String email) {
		invitationService.issue(email, adminId);
		return mailbox.lastTokenSentTo(email);
	}

	private static List<Object> runConcurrently(List<Callable<Object>> tasks, CountDownLatch startSignal)
			throws InterruptedException {
		try (ExecutorService executor = Executors.newFixedThreadPool(tasks.size())) {
			List<Future<Object>> futures = tasks.stream().map(executor::submit).toList();
			startSignal.countDown();
			List<Object> outcomes = new ArrayList<>();
			for (Future<Object> future : futures) {
				outcomes.add(outcomeOf(future));
			}
			return outcomes;
		}
	}

	private static Object outcomeOf(Future<Object> future) throws InterruptedException {
		try {
			return future.get();
		}
		catch (java.util.concurrent.ExecutionException failure) {
			return failure.getCause();
		}
	}

}
