package dev.vinicius.cursos.api.account.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Único ponto em que uma senha em texto vira hash. Aplica a política antes, para que nenhuma senha
 * fora dela chegue ao banco por outro caminho.
 */
@Component
public class PasswordHasher {

	private final PasswordEncoder passwordEncoder;

	PasswordHasher(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Valida a senha pela {@link PassphrasePolicy} e devolve o hash com salt no formato {@code {id}hash}.
	 *
	 * <p>Exemplo: {@code String passwordHash = passwordHasher.hashAcceptable(rawPassword);}
	 */
	public String hashAcceptable(String rawPassword) {
		PassphrasePolicy.requireAcceptable(rawPassword);
		return passwordEncoder.encode(rawPassword);
	}

}
