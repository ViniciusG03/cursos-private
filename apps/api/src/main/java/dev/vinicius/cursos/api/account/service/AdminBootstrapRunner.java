package dev.vinicius.cursos.api.account.service;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

/**
 * Executa o bootstrap do administrador na inicialização, somente quando
 * {@code library.admin-bootstrap.enabled=true}. Falta de e-mail/senha ou conflito derruba a
 * inicialização: é melhor falhar alto do que subir sem administrador ou com um administrador errado.
 */
@Component
@ConditionalOnBooleanProperty(prefix = "library.admin-bootstrap", name = "enabled")
class AdminBootstrapRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

	private final AdminBootstrapService bootstrapService;

	private final AdminBootstrapProperties bootstrapProperties;

	AdminBootstrapRunner(AdminBootstrapService bootstrapService, AdminBootstrapProperties bootstrapProperties) {
		this.bootstrapService = bootstrapService;
		this.bootstrapProperties = bootstrapProperties;
	}

	@Override
	public void run(ApplicationArguments arguments) {
		requireEmailAndPassword();
		AdminBootstrapOutcome outcome = bootstrapService.bootstrap(AccountEmail.parse(bootstrapProperties.email()),
				bootstrapProperties.password());
		log.atInfo()
			.addKeyValue("adminId", outcome.adminId())
			.addKeyValue("created", outcome.created())
			.log(outcome.created() ? "admin bootstrap created the admin account"
					: "admin bootstrap found the admin account; password left unchanged");
	}

	private void requireEmailAndPassword() {
		boolean emailMissing = isBlank(bootstrapProperties.email());
		boolean passwordMissing = isBlank(bootstrapProperties.password());
		if (emailMissing || passwordMissing) {
			throw new IllegalStateException(
					"admin bootstrap enabled: expected LIBRARY_ADMIN_BOOTSTRAP_EMAIL and LIBRARY_ADMIN_BOOTSTRAP_PASSWORD, got email=%s, password=%s"
						.formatted(emailMissing ? "missing" : bootstrapProperties.email(),
								passwordMissing ? "missing" : "set"));
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
