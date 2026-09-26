package dev.vinicius.cursos.api.passwordreset.service;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.account.domain.UserAccount;
import dev.vinicius.cursos.api.account.repository.UserAccountRepository;
import dev.vinicius.cursos.api.onetimetoken.OneTimeTokenDigest;
import dev.vinicius.cursos.api.onetimetoken.OneTimeTokenGenerator;
import dev.vinicius.cursos.api.passwordreset.domain.PasswordResetToken;
import dev.vinicius.cursos.api.passwordreset.repository.PasswordResetTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emissão do token de recuperação. Roda dentro da transação do {@link PasswordResetDispatcher}, junto com
 * o envio: se o e-mail falhar, o token emitido é revogado na mesma transação e nenhum link não entregue
 * fica válido.
 */
@Component
class PasswordResetRegistry {

	private final UserAccountRepository accountRepository;

	private final PasswordResetTokenRepository resetTokenRepository;

	private final OneTimeTokenGenerator tokenGenerator;

	private final Clock clock;

	PasswordResetRegistry(UserAccountRepository accountRepository, PasswordResetTokenRepository resetTokenRepository,
			OneTimeTokenGenerator tokenGenerator, Clock clock) {
		this.accountRepository = accountRepository;
		this.resetTokenRepository = resetTokenRepository;
		this.tokenGenerator = tokenGenerator;
		this.clock = clock;
	}

	/**
	 * Revoga tokens anteriores da conta e emite um novo. Vazio quando não há conta com o e-mail: quem
	 * chama responde igual nos dois casos.
	 *
	 * <p>Exemplo: {@code Optional<IssuedPasswordReset> issued = registry.issueFor(email);}
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	Optional<IssuedPasswordReset> issueFor(AccountEmail email) {
		// Trava a conta: dois pedidos da mesma pessoa processados ao mesmo tempo emitem um depois do outro
		// (o último token vale), em vez de colidirem em uq_password_reset_tokens_open_user.
		return accountRepository.lockByEmail(email.value()).map(this::replaceOpenToken);
	}

	/**
	 * Revoga o token de um link que não foi entregue, na mesma transação que o emitiu.
	 *
	 * <p>Exemplo: {@code registry.revokeUndelivered(issued, now);}
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	void revokeUndelivered(IssuedPasswordReset issued, Instant now) {
		resetTokenRepository.findById(issued.resetTokenId()).ifPresent(resetToken -> resetToken.revokeIfOpen(now));
	}

	private IssuedPasswordReset replaceOpenToken(UserAccount account) {
		Instant now = clock.instant();
		resetTokenRepository.revokeOpenForUser(account.getId(), now);
		String rawToken = tokenGenerator.generate();
		PasswordResetToken resetToken = PasswordResetToken.issue(account.getId(),
				OneTimeTokenDigest.sha256Hex(rawToken), now);
		resetTokenRepository.saveAndFlush(resetToken);
		return new IssuedPasswordReset(resetToken.getId(), account.getEmail(), rawToken, resetToken.getExpiresAt());
	}

}
