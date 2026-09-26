package dev.vinicius.cursos.api.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vinicius.cursos.api.catalog.CatalogIntegrationTest;
import dev.vinicius.cursos.api.catalog.domain.InvalidCatalogTitleException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CourseModuleOrganizerTest extends CatalogIntegrationTest {

	@Autowired
	private CourseModuleOrganizer moduleOrganizer;

	@Autowired
	private LessonOrganizer lessonOrganizer;

	@Autowired
	private CourseLifecycleService lifecycleService;

	private UUID courseId;

	@BeforeEach
	void createCourse() {
		courseId = insertCourseRow("Curso");
	}

	@Test
	void appendsModulesAtTheEndStartingAtOne() {
		UUID firstId = moduleOrganizer.appendModule(courseId, "Primeiro");
		UUID secondId = moduleOrganizer.appendModule(courseId, "Segundo");
		UUID thirdId = moduleOrganizer.appendModule(courseId, "Terceiro");

		assertThat(readModuleIdsByPosition(courseId)).containsExactly(firstId, secondId, thirdId);
		assertThat(readModulePositions(courseId)).containsExactly(1, 2, 3);
	}

	@Test
	void appendsAfterHighestPositionEvenWithGaps() {
		insertModuleRow(courseId, "Inserido via SQL", 5);

		moduleOrganizer.appendModule(courseId, "Novo");

		assertThat(readModulePositions(courseId)).containsExactly(5, 6);
	}

	@Test
	void positionsAreIndependentPerCourse() {
		UUID otherCourseId = insertCourseRow("Outro");
		moduleOrganizer.appendModule(courseId, "A");
		moduleOrganizer.appendModule(otherCourseId, "B");

		assertThat(readModulePositions(courseId)).containsExactly(1);
		assertThat(readModulePositions(otherCourseId)).containsExactly(1);
	}

	@Test
	void rejectsModuleForUnknownCourse() {
		UUID unknownCourseId = UUID.randomUUID();

		assertThatThrownBy(() -> moduleOrganizer.appendModule(unknownCourseId, "Módulo"))
			.isInstanceOf(CatalogItemNotFoundException.class)
			.hasMessageContaining("course not found")
			.hasMessageContaining(unknownCourseId.toString());
	}

	@Test
	void rejectsBlankModuleTitle() {
		assertThatThrownBy(() -> moduleOrganizer.appendModule(courseId, ""))
			.isInstanceOf(InvalidCatalogTitleException.class)
			.hasMessageContaining("module title");
		assertThat(readModulePositions(courseId)).isEmpty();
	}

	@Test
	void rejectsAppendingModuleToPublishedCourseWithoutChangingIt() {
		UUID publishedCourseId = lifecycleService.createDraft("Publicado", null);
		lessonOrganizer.appendLesson(moduleOrganizer.appendModule(publishedCourseId, "Módulo 1"), "Aula 1");
		lifecycleService.publish(publishedCourseId);
		List<UUID> modulesBefore = readModuleIdsByPosition(publishedCourseId);

		assertThatThrownBy(() -> moduleOrganizer.appendModule(publishedCourseId, "Módulo vazio"))
			.isInstanceOf(PublishedCourseModificationException.class)
			.hasMessageContaining(publishedCourseId.toString())
			.hasMessageContaining("expected status DRAFT, got PUBLISHED");
		assertThat(readCourseStatus(publishedCourseId)).isEqualTo("PUBLISHED");
		assertThat(readModuleIdsByPosition(publishedCourseId)).isEqualTo(modulesBefore);
		assertThat(readModulePositions(publishedCourseId)).containsExactly(1);
	}

	@Test
	void swapsTwoModulesAndPersists() {
		UUID firstId = moduleOrganizer.appendModule(courseId, "Primeiro");
		UUID secondId = moduleOrganizer.appendModule(courseId, "Segundo");

		moduleOrganizer.reorderModules(courseId, List.of(secondId, firstId));

		assertThat(readModuleIdsByPosition(courseId)).containsExactly(secondId, firstId);
		assertThat(readModulePositions(courseId)).containsExactly(1, 2);
	}

	@Test
	void reversesModulesAndCompactsGaps() {
		UUID firstId = insertModuleRow(courseId, "A", 2);
		UUID secondId = insertModuleRow(courseId, "B", 4);
		UUID thirdId = insertModuleRow(courseId, "C", 7);

		moduleOrganizer.reorderModules(courseId, List.of(thirdId, secondId, firstId));

		assertThat(readModuleIdsByPosition(courseId)).containsExactly(thirdId, secondId, firstId);
		assertThat(readModulePositions(courseId)).containsExactly(1, 2, 3);
	}

	@Test
	void rejectsModuleFromAnotherCourseWithoutChangingPositions() {
		UUID firstId = moduleOrganizer.appendModule(courseId, "Primeiro");
		moduleOrganizer.appendModule(courseId, "Segundo");
		UUID foreignId = moduleOrganizer.appendModule(insertCourseRow("Outro"), "Alheio");
		List<UUID> orderBefore = readModuleIdsByPosition(courseId);

		assertThatThrownBy(() -> moduleOrganizer.reorderModules(courseId, List.of(foreignId, firstId)))
			.isInstanceOf(InvalidReorderException.class)
			.hasMessageContaining(foreignId.toString());
		assertThat(readModuleIdsByPosition(courseId)).isEqualTo(orderBefore);
		assertThat(readModulePositions(courseId)).containsExactly(1, 2);
	}

	@Test
	void rejectsDuplicatedModuleIdWithoutChangingPositions() {
		UUID firstId = moduleOrganizer.appendModule(courseId, "Primeiro");
		moduleOrganizer.appendModule(courseId, "Segundo");
		List<UUID> orderBefore = readModuleIdsByPosition(courseId);

		assertThatThrownBy(() -> moduleOrganizer.reorderModules(courseId, List.of(firstId, firstId)))
			.isInstanceOf(InvalidReorderException.class);
		assertThat(readModuleIdsByPosition(courseId)).isEqualTo(orderBefore);
	}

	@Test
	void rejectsPartialModuleListWithoutChangingPositions() {
		moduleOrganizer.appendModule(courseId, "Primeiro");
		UUID secondId = moduleOrganizer.appendModule(courseId, "Segundo");
		List<UUID> orderBefore = readModuleIdsByPosition(courseId);

		assertThatThrownBy(() -> moduleOrganizer.reorderModules(courseId, List.of(secondId)))
			.isInstanceOf(InvalidReorderException.class);
		assertThat(readModuleIdsByPosition(courseId)).isEqualTo(orderBefore);
	}

	@Test
	void rejectsReorderOfUnknownCourse() {
		UUID unknownCourseId = UUID.randomUUID();

		assertThatThrownBy(() -> moduleOrganizer.reorderModules(unknownCourseId, List.of()))
			.isInstanceOf(CatalogItemNotFoundException.class)
			.hasMessageContaining(unknownCourseId.toString());
	}

}
