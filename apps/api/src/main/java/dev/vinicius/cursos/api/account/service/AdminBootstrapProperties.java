package dev.vinicius.cursos.api.account.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Entrada do bootstrap explícito ({@code library.admin-bootstrap.*}), normalmente via variáveis de
 * ambiente {@code LIBRARY_ADMIN_BOOTSTRAP_ENABLED/EMAIL/PASSWORD} (ver {@code application.yaml}). Sem
 * defaults de e-mail ou senha.
 *
 * <p>Exemplo: {@code properties.enabled() && properties.email() != null}.
 *
 * @param enabled liga o bootstrap nesta inicialização
 * @param email e-mail do administrador
 * @param password senha inicial (segredo do ambiente; nunca logada)
 */
@ConfigurationProperties(prefix = "library.admin-bootstrap")
public record AdminBootstrapProperties(boolean enabled, String email, String password) {

	@Override
	public String toString() {
		return "AdminBootstrapProperties[enabled=%s, email=%s, password=<omitted>]".formatted(enabled, email);
	}

}
