package dev.vinicius.cursos.api.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vinicius.cursos.api.security.PasswordEncoderConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordHasherTest {

	private final PasswordEncoder passwordEncoder = PasswordEncoderConfiguration.libraryPasswordEncoder();

	private final PasswordHasher passwordHasher = new PasswordHasher(passwordEncoder);

	@Test
	void storesAlgorithmPrefixedSaltedHashNotThePassword() {
		String password = "cavalo correto bateria grampo";

		String firstHash = passwordHasher.hashAcceptable(password);
		String secondHash = passwordHasher.hashAcceptable(password);

		assertThat(firstHash).startsWith("{pbkdf2@SpringSecurity_v5_8}").doesNotContain(password);
		assertThat(firstHash).isNotEqualTo(secondHash);
		assertThat(passwordEncoder.matches(password, firstHash)).isTrue();
	}

	@Test
	void wrongPasswordDoesNotMatch() {
		String hash = passwordHasher.hashAcceptable("cavalo correto bateria grampo");

		assertThat(passwordEncoder.matches("cavalo correto bateria grampO", hash)).isFalse();
	}

	@Test
	void longMultibytePassphraseIsNotTruncated() {
		// Com BCrypt, qualquer coisa após 72 bytes seria ignorada; aqui o último caractere importa.
		String longPassphrase = "🔑".repeat(127) + "a";
		String hash = passwordHasher.hashAcceptable(longPassphrase);

		assertThat(passwordEncoder.matches(longPassphrase, hash)).isTrue();
		assertThat(passwordEncoder.matches("🔑".repeat(127) + "b", hash)).isFalse();
	}

	@Test
	void refusesToHashPasswordOutsidePolicy() {
		assertThatThrownBy(() -> passwordHasher.hashAcceptable("curta"))
			.isInstanceOf(PassphrasePolicyViolationException.class);
	}

}
