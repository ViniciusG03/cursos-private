package dev.vinicius.cursos.api.account.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PassphrasePolicyTest {

	@Test
	void acceptsTwelveCharacters() {
		assertThatCode(() -> PassphrasePolicy.requireAcceptable("a".repeat(12))).doesNotThrowAnyException();
	}

	@Test
	void rejectsElevenCharactersCitingLengthOnly() {
		assertThatThrownBy(() -> PassphrasePolicy.requireAcceptable("segredo1234"))
			.isInstanceOf(PassphrasePolicyViolationException.class)
			.hasMessageContaining("got 11")
			.hasMessageNotContaining("segredo1234");
	}

	@Test
	void accepts128MultibyteCodePoints() {
		// 128 emojis: 256 chars UTF-16 e 512 bytes UTF-8; a política conta code points.
		assertThatCode(() -> PassphrasePolicy.requireAcceptable("🔑".repeat(128))).doesNotThrowAnyException();
	}

	@Test
	void rejects129CodePointsInsteadOfTruncating() {
		assertThatThrownBy(() -> PassphrasePolicy.requireAcceptable("é".repeat(129)))
			.isInstanceOf(PassphrasePolicyViolationException.class)
			.hasMessageContaining("got 129");
	}

	@Test
	void rejectsNullAndBlank() {
		assertThatThrownBy(() -> PassphrasePolicy.requireAcceptable(null))
			.isInstanceOf(PassphrasePolicyViolationException.class);
		assertThatThrownBy(() -> PassphrasePolicy.requireAcceptable(" ".repeat(20)))
			.isInstanceOf(PassphrasePolicyViolationException.class);
	}

	@Test
	void keepsSurroundingSpacesAsPartOfThePassword() {
		assertThatCode(() -> PassphrasePolicy.requireAcceptable("  onze chars  ")).doesNotThrowAnyException();
	}

}
