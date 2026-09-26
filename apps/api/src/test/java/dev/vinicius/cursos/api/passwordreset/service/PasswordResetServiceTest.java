package dev.vinicius.cursos.api.passwordreset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.account.domain.UserAccount;
import dev.vinicius.cursos.api.account.service.PassphrasePolicyViolationException;
import dev.vinicius.cursos.api.onetimetoken.InvalidOneTimeTokenException;
import dev.vinicius.cursos.api.onetimetoken.OneTimeTokenDigest;
import dev.vinicius.cursos.api.passwordreset.domain.PasswordResetToken;
import dev.vinicius.cursos.api.support.AccessIntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Pedido e confirmação de recuperação; o e-mail chega pelo worker real, em outra thread. */
class PasswordResetServiceTest extends AccessIntegrationTest {

	private static final String NEW_PASSWORD = "nova senha escolhida agora";

	@Autowired
	private PasswordResetRequestService resetRequestService;

	@Autowired
	private PasswordResetConfirmation resetConfirmation;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private UserAccount member;

	@BeforeEach
	void createMemberAccount() {
		member = createMember("ana@x.com");
	}

	@Test
	void existingAccountReceivesLinkAndOnlyHashIsStored() {
		resetRequestService.requestReset(AccountEmail.parse("ANA@x.com"));

		String rawToken = awaitResetLinkTo("ana@x.com", 1);
		assertThat(jdbcTemplate.queryForList("SELECT token_hash FROM password_reset_tokens", String.class))
			.containsExactly(OneTimeTokenDigest.sha256Hex(rawToken));
	}

	@Test
	void unknownEmailIsQueuedLikeAnyOtherAndSendsNothing() throws Exception {
		resetRequestService.requestReset(AccountEmail.parse("ninguem@x.com"));
		awaitDueResetRequestsHandled();

		assertThat(mailbox.deliveredMails()).isEmpty();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM password_reset_tokens", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT status FROM password_reset_requests", String.class))
			.isEqualTo("NO_ACCOUNT");
	}

	@Test
	void confirmationChangesPasswordAndAdvancesCredentialVersion() {
		String rawToken = requestAndAwaitToken(1);
		clock.advance(Duration.ofMinutes(5));

		resetConfirmation.confirm(rawToken, NEW_PASSWORD);

		UserAccount updated = accountRepository.findById(member.getId()).orElseThrow();
		assertThat(passwordEncoder.matches(NEW_PASSWORD, updated.getPasswordHash())).isTrue();
		assertThat(passwordEncoder.matches(MEMBER_PASSWORD, updated.getPasswordHash())).isFalse();
		assertThat(updated.getCredentialVersion()).isEqualTo(member.getCredentialVersion() + 1);
		assertThat(updated.getPasswordChangedAt()).isEqualTo(clock.instant());
	}

	@Test
	void newRequestRevokesPreviousToken() {
		String firstToken = requestAndAwaitToken(1);
		String secondToken = requestAndAwaitToken(2);

		assertThatThrownBy(() -> resetConfirmation.confirm(firstToken, NEW_PASSWORD))
			.isInstanceOf(InvalidOneTimeTokenException.class);
		resetConfirmation.confirm(secondToken, NEW_PASSWORD);
	}

	@Test
	void tokenIsSingleUse() {
		String rawToken = requestAndAwaitToken(1);
		resetConfirmation.confirm(rawToken, NEW_PASSWORD);

		assertThatThrownBy(() -> resetConfirmation.confirm(rawToken, "mais uma senha longa aqui"))
			.isInstanceOf(InvalidOneTimeTokenException.class);
	}

	@Test
	void usableOneSecondBeforeThirtyMinutes() {
		String rawToken = requestAndAwaitToken(1);
		clock.advance(PasswordResetToken.VALIDITY.minusSeconds(1));

		resetConfirmation.confirm(rawToken, NEW_PASSWORD);
	}

	@Test
	void expiredExactlyAtThirtyMinutes() {
		String rawToken = requestAndAwaitToken(1);
		clock.advance(Duration.ofMinutes(30));

		assertThatThrownBy(() -> resetConfirmation.confirm(rawToken, NEW_PASSWORD))
			.isInstanceOf(InvalidOneTimeTokenException.class);
		assertThat(accountRepository.findById(member.getId()).orElseThrow().getCredentialVersion()).isZero();
	}

	@Test
	void weakPasswordDoesNotConsumeToken() {
		String rawToken = requestAndAwaitToken(1);

		assertThatThrownBy(() -> resetConfirmation.confirm(rawToken, "curta"))
			.isInstanceOf(PassphrasePolicyViolationException.class);
		resetConfirmation.confirm(rawToken, NEW_PASSWORD);
	}

	private String requestAndAwaitToken(int mailNumber) {
		resetRequestService.requestReset(AccountEmail.parse("ana@x.com"));
		return awaitResetLinkTo("ana@x.com", mailNumber);
	}

}
