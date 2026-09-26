package dev.vinicius.cursos.api.onetimetoken;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Tokens de 256 bits vindos de {@link SecureRandom}, codificados em Base64 URL sem padding
 * (43 caracteres). Nunca usar {@code java.util.Random}: o token é a única prova de posse do e-mail.
 */
@Component
public class SecureRandomTokenGenerator implements OneTimeTokenGenerator {

	static final int TOKEN_BYTES = 32;

	private final SecureRandom secureRandom;

	public SecureRandomTokenGenerator() {
		this(new SecureRandom());
	}

	SecureRandomTokenGenerator(SecureRandom secureRandom) {
		this.secureRandom = secureRandom;
	}

	/**
	 * Gera 32 bytes aleatórios e os codifica para uso direto em links.
	 *
	 * <p>Exemplo: {@code new SecureRandomTokenGenerator().generate().length() == 43}.
	 */
	@Override
	public String generate() {
		byte[] randomBytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

}
