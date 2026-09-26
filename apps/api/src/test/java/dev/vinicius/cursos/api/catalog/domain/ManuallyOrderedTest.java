package dev.vinicius.cursos.api.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ManuallyOrderedTest {

	@ParameterizedTest
	@ValueSource(ints = { 0, -1 })
	void rejectsNonPositivePosition(int invalidPosition) {
		assertThatThrownBy(() -> ManuallyOrdered.requirePositive(invalidPosition))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("got: " + invalidPosition);
	}

	@Test
	void acceptsPositivePosition() {
		assertThat(ManuallyOrdered.requirePositive(1)).isEqualTo(1);
	}

}
