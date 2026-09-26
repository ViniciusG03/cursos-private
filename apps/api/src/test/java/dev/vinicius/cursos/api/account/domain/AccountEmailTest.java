package dev.vinicius.cursos.api.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AccountEmailTest {

	@Test
	void normalizesCaseAndSurroundingSpaces() {
		assertThat(AccountEmail.parse("  Ana.Silva@Example.COM ").value()).isEqualTo("ana.silva@example.com");
	}

	@Test
	void differentlyTypedEmailsAreTheSameIdentity() {
		assertThat(AccountEmail.parse("ANA@x.com")).isEqualTo(AccountEmail.parse("ana@X.com"));
	}

	@Test
	void rejectsNull() {
		assertThatThrownBy(() -> AccountEmail.parse(null)).isInstanceOf(InvalidAccountEmailException.class)
			.hasMessageContaining("got: null");
	}

	@Test
	void rejectsValueWithoutAtSign() {
		assertThatThrownBy(() -> AccountEmail.parse("ana.example.com")).isInstanceOf(InvalidAccountEmailException.class)
			.hasMessageContaining("'ana.example.com'");
	}

	@Test
	void rejectsInnerWhitespace() {
		assertThatThrownBy(() -> AccountEmail.parse("ana silva@x.com")).isInstanceOf(InvalidAccountEmailException.class);
	}

	@Test
	void rejectsEmailLongerThanColumn() {
		String tooLong = "a".repeat(250) + "@x.com";

		assertThatThrownBy(() -> AccountEmail.parse(tooLong)).isInstanceOf(InvalidAccountEmailException.class)
			.hasMessageContaining("at most 254");
	}

	@Test
	void constructorRejectsNonNormalizedValue() {
		assertThatThrownBy(() -> new AccountEmail("Ana@x.com")).isInstanceOf(InvalidAccountEmailException.class)
			.hasMessageContaining("normalized");
	}

}
