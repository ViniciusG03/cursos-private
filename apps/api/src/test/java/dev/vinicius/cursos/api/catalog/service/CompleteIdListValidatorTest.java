package dev.vinicius.cursos.api.catalog.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompleteIdListValidatorTest {

	private final UUID parentId = UUID.randomUUID();

	private final UUID firstId = UUID.randomUUID();

	private final UUID secondId = UUID.randomUUID();

	private final List<UUID> currentIds = List.of(firstId, secondId);

	@Test
	void acceptsPermutationOfCurrentIds() {
		assertThatCode(() -> CompleteIdListValidator.requireSameIds("course", parentId, currentIds,
				List.of(secondId, firstId)))
			.doesNotThrowAnyException();
	}

	@Test
	void acceptsEmptyListForParentWithoutChildren() {
		assertThatCode(() -> CompleteIdListValidator.requireSameIds("module", parentId, List.of(), List.of()))
			.doesNotThrowAnyException();
	}

	@Test
	void rejectsNullList() {
		assertThatThrownBy(() -> CompleteIdListValidator.requireSameIds("course", parentId, currentIds, null))
			.isInstanceOf(InvalidReorderException.class)
			.hasMessageContaining("got null");
	}

	@Test
	void rejectsDuplicatedIdEvenWithCorrectSize() {
		assertThatThrownBy(() -> CompleteIdListValidator.requireSameIds("course", parentId, currentIds,
				List.of(firstId, firstId)))
			.isInstanceOf(InvalidReorderException.class)
			.hasMessageContaining("duplicated=[" + firstId + "]")
			.hasMessageContaining("missing=[" + secondId + "]");
	}

	@Test
	void rejectsUnknownIdEvenWithCorrectSize() {
		UUID unknownId = UUID.randomUUID();

		assertThatThrownBy(() -> CompleteIdListValidator.requireSameIds("course", parentId, currentIds,
				List.of(firstId, unknownId)))
			.isInstanceOf(InvalidReorderException.class)
			.hasMessageContaining("not in this course=[" + unknownId + "]");
	}

	@Test
	void rejectsPartialList() {
		assertThatThrownBy(() -> CompleteIdListValidator.requireSameIds("module", parentId, currentIds,
				List.of(secondId)))
			.isInstanceOf(InvalidReorderException.class)
			.hasMessageContaining("missing=[" + firstId + "]");
	}

}
