package dev.vinicius.cursos.api.onetimetoken;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SecureRandomTokenGeneratorTest {

	private final SecureRandomTokenGenerator tokenGenerator = new SecureRandomTokenGenerator();

	@Test
	void generatesUrlSafeTokenWith256Bits() {
		String token = tokenGenerator.generate();

		assertThat(token).matches("[A-Za-z0-9_-]{43}");
		assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
	}

	@Test
	void generatesDistinctTokens() {
		Set<String> tokens = new HashSet<>();
		for (int attempt = 0; attempt < 1_000; attempt++) {
			tokens.add(tokenGenerator.generate());
		}

		assertThat(tokens).hasSize(1_000);
	}

}
