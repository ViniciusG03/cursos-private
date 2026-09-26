package dev.vinicius.cursos.api.onetimetoken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OneTimeTokenDigestTest {

	@Test
	void matchesKnownSha256Vector() {
		assertThat(OneTimeTokenDigest.sha256Hex("abc"))
			.isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
	}

	@Test
	void missingTokenIsTreatedAsInvalidToken() {
		assertThatThrownBy(() -> OneTimeTokenDigest.sha256Hex(null)).isInstanceOf(InvalidOneTimeTokenException.class);
		assertThatThrownBy(() -> OneTimeTokenDigest.sha256Hex("  ")).isInstanceOf(InvalidOneTimeTokenException.class);
	}

}
