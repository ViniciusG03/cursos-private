package dev.vinicius.cursos.api.invitation.service;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.account.domain.UserAccount;
import dev.vinicius.cursos.api.account.repository.UserAccountRepository;
import dev.vinicius.cursos.api.account.service.PassphrasePolicy;
import dev.vinicius.cursos.api.account.service.PasswordHasher;
import dev.vinicius.cursos.api.invitation.domain.Invitation;
import dev.vinicius.cursos.api.invitation.repository.InvitationEmailLock;
import dev.vinicius.cursos.api.invitation.repository.InvitationRepository;
import dev.vinicius.cursos.api.onetimetoken.InvalidOneTimeTokenException;
import dev.vinicius.cursos.api.onetimetoken.OneTimeTokenDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aceite de convite: consome o token e cria o {@code MEMBER} com a senha escolhida por ele. */
@Service
public class InvitationAcceptance {

	private final InvitationRepository invitationRepository;

	private final UserAccountRepository accountRepository;

	private final InvitationEmailLock invitationEmailLock;

	private final PasswordHasher passwordHasher;

	private final Clock clock;

	InvitationAcceptance(InvitationRepository invitationRepository, UserAccountRepository accountRepository,
			InvitationEmailLock invitationEmailLock, PasswordHasher passwordHasher, Clock clock) {
		this.invitationRepository = invitationRepository;
		this.accountRepository = accountRepository;
		this.invitationEmailLock = invitationEmailLock;
		this.passwordHasher = passwordHasher;
		this.clock = clock;
	}

	/**
	 * Consome o convite e cria a conta na mesma transação. A senha é validada antes, para que uma senha
	 * fraca não gaste o convite; a sessão não é criada aqui (o convidado faz login em seguida). Pega o lock
	 * por e-mail antes do lock da linha, na mesma ordem do reenvio, para coordenar com ele sem deadlock.
	 *
	 * <p>Exemplo: {@code UUID memberId = acceptance.accept(rawTokenFromLink, "minha senha longa e boa");}
	 */
	@Transactional
	public UUID accept(String rawToken, String chosenPassword) {
		PassphrasePolicy.requireAcceptable(chosenPassword);
		String tokenHash = OneTimeTokenDigest.sha256Hex(rawToken);
		AccountEmail invitedEmail = invitationRepository.findEmailByTokenHash(tokenHash)
			.map(AccountEmail::new)
			.orElseThrow(InvalidOneTimeTokenException::new);
		invitationEmailLock.lockFor(invitedEmail);
		Invitation invitation = invitationRepository.lockByTokenHash(tokenHash)
			.orElseThrow(InvalidOneTimeTokenException::new);
		Instant now = clock.instant();
		invitation.consume(now);
		return createMember(invitation.getEmail(), chosenPassword, now);
	}

	private UUID createMember(AccountEmail email, String chosenPassword, Instant now) {
		if (accountRepository.existsByEmail(email.value())) {
			throw new InvitationConflictException(
					"cannot accept invitation for %s: expected no account yet, got an active one".formatted(email));
		}
		UserAccount member = UserAccount.member(email, passwordHasher.hashAcceptable(chosenPassword), now);
		return accountRepository.saveAndFlush(member).getId();
	}

}
