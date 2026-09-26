package dev.vinicius.cursos.api.security;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

/**
 * Hash de senhas com PBKDF2-HMAC-SHA256 (310 mil iterações, salt aleatório) dentro de um
 * {@link DelegatingPasswordEncoder}, que grava o algoritmo no prefixo {@code {id}} e permite migrar no
 * futuro. Não é BCrypt porque BCrypt só considera 72 bytes e a política aceita 128 caracteres.
 */
@Configuration(proxyBeanMethods = false)
public class PasswordEncoderConfiguration {

	static final String PBKDF2_ENCODER_ID = "pbkdf2@SpringSecurity_v5_8";

	/**
	 * Encoder usado no cadastro e no login.
	 *
	 * <p>Exemplo: {@code PasswordEncoderConfiguration.libraryPasswordEncoder().encode(raw)}.
	 */
	@Bean
	public static PasswordEncoder libraryPasswordEncoder() {
		return new DelegatingPasswordEncoder(PBKDF2_ENCODER_ID,
				Map.of(PBKDF2_ENCODER_ID, Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
	}

}
