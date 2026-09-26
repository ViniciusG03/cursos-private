package dev.vinicius.cursos.api.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CatalogTitleTest {

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "   ", "\t\n" })
	void rejectsMissingOrBlankTitle(String blankTitle) {
		assertThatThrownBy(() -> CatalogTitle.requireValid(blankTitle, 200, "lesson title"))
			.isInstanceOf(InvalidCatalogTitleException.class)
			.hasMessageContaining("lesson title must contain non-blank text");
	}

	@Test
	void rejectsTitleLongerThanColumn() {
		String longTitle = "a".repeat(201);

		assertThatThrownBy(() -> CatalogTitle.requireValid(longTitle, 200, "module title"))
			.isInstanceOf(InvalidCatalogTitleException.class)
			.hasMessageContaining("at most 200 characters, got 201");
	}

	@Test
	void stripsSurroundingSpaces() {
		assertThat(CatalogTitle.requireValid("  Introdução  ", 200, "module title")).isEqualTo("Introdução");
	}

}
