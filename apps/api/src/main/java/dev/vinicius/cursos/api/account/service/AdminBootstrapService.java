package dev.vinicius.cursos.api.account.service;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.account.domain.UserAccount;
import dev.vinicius.cursos.api.account.domain.UserRole;
import dev.vinicius.cursos.api.account.repository.UserAccountRepository;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Criação idempotente do primeiro administrador. Não existe conta padrão: a senha vem do ambiente no
 * momento do bootstrap e, se o administrador já existe, ela nunca é trocada por aqui.
 */
@Service
public class AdminBootstrapService {

	private final UserAccountRepository accountRepository;

	private final PasswordHasher passwordHasher;

	private final Clock clock;

	AdminBootstrapService(UserAccountRepository accountRepository, PasswordHasher passwordHasher, Clock clock) {
		this.accountRepository = accountRepository;
		this.passwordHasher = passwordHasher;
		this.clock = clock;
	}

	/**
	 * Cria o ADMIN se ainda não houver um. Repetir com o mesmo e-mail não altera nada; outro e-mail, ou um
	 * e-mail de membro, é conflito.
	 *
	 * <p>Exemplo: {@code bootstrapService.bootstrap(AccountEmail.parse("adm@x.com"), senhaDoAmbiente)}.
	 */
	@Transactional
	public AdminBootstrapOutcome bootstrap(AccountEmail email, String initialPassword) {
		PassphrasePolicy.requireAcceptable(initialPassword);
		Optional<UserAccount> existingAdmin = accountRepository.findFirstByRole(UserRole.ADMIN);
		if (existingAdmin.isPresent()) {
			return keepExistingAdmin(existingAdmin.get(), email);
		}
		if (accountRepository.existsByEmail(email.value())) {
			throw new AdminBootstrapConflictException(
					"cannot bootstrap admin %s: expected an unused email, got an existing member".formatted(email));
		}
		UserAccount admin = UserAccount.admin(email, passwordHasher.hashAcceptable(initialPassword), clock.instant());
		return new AdminBootstrapOutcome(accountRepository.saveAndFlush(admin).getId(), true);
	}

	private static AdminBootstrapOutcome keepExistingAdmin(UserAccount existingAdmin, AccountEmail requestedEmail) {
		if (!existingAdmin.getEmail().equals(requestedEmail)) {
			throw new AdminBootstrapConflictException("cannot bootstrap admin %s: expected the existing admin %s"
				.formatted(requestedEmail, existingAdmin.getEmail()));
		}
		return new AdminBootstrapOutcome(existingAdmin.getId(), false);
	}

}
