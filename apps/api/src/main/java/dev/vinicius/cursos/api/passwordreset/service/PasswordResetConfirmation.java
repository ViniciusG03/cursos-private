package dev.vinicius.cursos.api.passwordreset.service;

import dev.vinicius.cursos.api.account.domain.UserAccount;
import dev.vinicius.cursos.api.account.repository.UserAccountRepository;
import dev.vinicius.cursos.api.account.service.PassphrasePolicy;
import dev.vinicius.cursos.api.account.service.PasswordHasher;
import dev.vinicius.cursos.api.onetimetoken.InvalidOneTimeTokenException;
import dev.vinicius.cursos.api.onetimetoken.OneTimeTokenDigest;
import dev.vinicius.cursos.api.passwordreset.domain.PasswordResetToken;
import dev.vinicius.cursos.api.passwordreset.repository.PasswordResetTokenRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Confirmação da recuperação: consome o token e troca a senha na mesma transação. */
@Service
public class PasswordResetConfirmation {

	private final PasswordResetTokenRepository resetTokenRepository;

	private final UserAccountRepository accountRepository;

	private final PasswordHasher passwordHasher;

	private final Clock clock;

	PasswordResetConfirmation(PasswordResetTokenRepository resetTokenRepository,
			UserAccountRepository accountRepository, PasswordHasher passwordHasher, Clock clock) {
		this.resetTokenRepository = resetTokenRepository;
		this.accountRepository = accountRepository;
		this.passwordHasher = passwordHasher;
		this.clock = clock;
	}

	/**
	 * Troca a senha e avança {@code credential_version}, o que derruba as sessões abertas antes. Não cria
	 * sessão: o usuário entra de novo com a senha nova. Trava a conta antes do token, na mesma ordem do
	 * worker que emite tokens; a ordem inversa causava deadlock com um pedido processado ao mesmo tempo.
	 *
	 * <p>Exemplo: {@code confirmation.confirm(rawTokenFromLink, "outra senha longa e boa");}
	 */
	@Transactional
	public void confirm(String rawToken, String newPassword) {
		PassphrasePolicy.requireAcceptable(newPassword);
		String tokenHash = OneTimeTokenDigest.sha256Hex(rawToken);
		UserAccount account = resetTokenRepository.findUserIdByTokenHash(tokenHash)
			.flatMap(accountRepository::lockById)
			.orElseThrow(InvalidOneTimeTokenException::new);
		PasswordResetToken resetToken = resetTokenRepository.lockByTokenHash(tokenHash)
			.orElseThrow(InvalidOneTimeTokenException::new);
		Instant now = clock.instant();
		resetToken.consume(now);
		account.replacePasswordHash(passwordHasher.hashAcceptable(newPassword), now);
	}

}
