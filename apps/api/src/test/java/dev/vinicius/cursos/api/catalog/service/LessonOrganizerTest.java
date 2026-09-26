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

class LessonOrganizerTest extends CatalogIntegrationTest {

	@Autowired
	private LessonOrganizer lessonOrganizer;

	@Autowired
	private CourseLifecycleService lifecycleService;

	private UUID courseId;

	private UUID moduleId;

	@BeforeEach
	void createModule() {
		courseId = insertCourseRow("Curso");
		moduleId = insertModuleRow(courseId, "Módulo", 1);
	}

	@Test
	void appendsLessonsAtTheEndStartingAtOne() {
		UUID firstId = lessonOrganizer.appendLesson(moduleId, "Primeira");
		UUID secondId = lessonOrganizer.appendLesson(moduleId, "Segunda");

		assertThat(readLessonIdsByPosition(moduleId)).containsExactly(firstId, secondId);
		assertThat(readLessonPositions(moduleId)).containsExactly(1, 2);
	}

	@Test
	void lessonPositionBelongsToModuleNotCourse() {
		UUID secondModuleId = insertModuleRow(courseId, "Módulo 2", 2);
		lessonOrganizer.appendLesson(moduleId, "1.1");
		lessonOrganizer.appendLesson(moduleId, "1.2");

		lessonOrganizer.appendLesson(secondModuleId, "2.1");

		assertThat(readLessonPositions(secondModuleId)).containsExactly(1);
	}

	@Test
	void rejectsLessonForUnknownModule() {
		UUID unknownModuleId = UUID.randomUUID();

		assertThatThrownBy(() -> lessonOrganizer.appendLesson(unknownModuleId, "Aula"))
			.isInstanceOf(CatalogItemNotFoundException.class)
			.hasMessageContaining("module not found")
			.hasMessageContaining(unknownModuleId.toString());
	}

	@Test
	void rejectsBlankLessonTitle() {
		assertThatThrownBy(() -> lessonOrganizer.appendLesson(moduleId, " \t "))
			.isInstanceOf(InvalidCatalogTitleException.class)
			.hasMessageContaining("lesson title");
		assertThat(readLessonPositions(moduleId)).isEmpty();
	}

	@Test
	void allowsAppendingLessonToPublishedCourseKeepingItPublished() {
		// Decisão registrada: diferente de um módulo novo, uma aula a mais não deixa o curso incompleto.
		UUID publishedCourseId = lifecycleService.createDraft("Publicado", null);
		UUID publishedModuleId = insertModuleRow(publishedCourseId, "Módulo 1", 1);
		UUID firstLessonId = lessonOrganizer.appendLesson(publishedModuleId, "Aula 1");
		lifecycleService.publish(publishedCourseId);

		UUID addedLessonId = lessonOrganizer.appendLesson(publishedModuleId, "Aula 2");

		assertThat(readCourseStatus(publishedCourseId)).isEqualTo("PUBLISHED");
		assertThat(readLessonIdsByPosition(publishedModuleId)).containsExactly(firstLessonId, addedLessonId);
		assertThat(readLessonPositions(publishedModuleId)).containsExactly(1, 2);
	}

	@Test
	void swapsTwoLessonsAndPersists() {
		UUID firstId = lessonOrganizer.appendLesson(moduleId, "Primeira");
		UUID secondId = lessonOrganizer.appendLesson(moduleId, "Segunda");

		lessonOrganizer.reorderLessons(moduleId, List.of(secondId, firstId));

		assertThat(readLessonIdsByPosition(moduleId)).containsExactly(secondId, firstId);
		assertThat(readLessonPositions(moduleId)).containsExactly(1, 2);
	}

	@Test
	void reversesFourLessonsAndPersists() {
		UUID firstId = lessonOrganizer.appendLesson(moduleId, "A");
		UUID secondId = lessonOrganizer.appendLesson(moduleId, "B");
		UUID thirdId = lessonOrganizer.appendLesson(moduleId, "C");
		UUID fourthId = lessonOrganizer.appendLesson(moduleId, "D");

		lessonOrganizer.reorderLessons(moduleId, List.of(fourthId, thirdId, secondId, firstId));

		assertThat(readLessonIdsByPosition(moduleId)).containsExactly(fourthId, thirdId, secondId, firstId);
		assertThat(readLessonPositions(moduleId)).containsExactly(1, 2, 3, 4);
	}

	@Test
	void rejectsLessonFromAnotherModuleWithoutChangingPositions() {
		UUID firstId = lessonOrganizer.appendLesson(moduleId, "Primeira");
		lessonOrganizer.appendLesson(moduleId, "Segunda");
		UUID foreignId = lessonOrganizer.appendLesson(insertModuleRow(courseId, "Outro", 2), "Alheia");
		List<UUID> orderBefore = readLessonIdsByPosition(moduleId);

		assertThatThrownBy(() -> lessonOrganizer.reorderLessons(moduleId, List.of(foreignId, firstId)))
			.isInstanceOf(InvalidReorderException.class)
			.hasMessageContaining(foreignId.toString());
		assertThat(readLessonIdsByPosition(moduleId)).isEqualTo(orderBefore);
		assertThat(readLessonPositions(moduleId)).containsExactly(1, 2);
	}

	@Test
	void rejectsUnknownLessonIdWithoutChangingPositions() {
		UUID firstId = lessonOrganizer.appendLesson(moduleId, "Primeira");
		lessonOrganizer.appendLesson(moduleId, "Segunda");
		List<UUID> orderBefore = readLessonIdsByPosition(moduleId);

		assertThatThrownBy(() -> lessonOrganizer.reorderLessons(moduleId, List.of(UUID.randomUUID(), firstId)))
			.isInstanceOf(InvalidReorderException.class);
		assertThat(readLessonIdsByPosition(moduleId)).isEqualTo(orderBefore);
	}

	@Test
	void rejectsDuplicatedAndPartialLessonListsWithoutChangingPositions() {
		UUID firstId = lessonOrganizer.appendLesson(moduleId, "Primeira");
		lessonOrganizer.appendLesson(moduleId, "Segunda");
		List<UUID> orderBefore = readLessonIdsByPosition(moduleId);

		assertThatThrownBy(() -> lessonOrganizer.reorderLessons(moduleId, List.of(firstId, firstId)))
			.isInstanceOf(InvalidReorderException.class);
		assertThatThrownBy(() -> lessonOrganizer.reorderLessons(moduleId, List.of(firstId)))
			.isInstanceOf(InvalidReorderException.class);
		assertThat(readLessonIdsByPosition(moduleId)).isEqualTo(orderBefore);
	}

	@Test
	void rejectsReorderOfUnknownModule() {
		UUID unknownModuleId = UUID.randomUUID();

		assertThatThrownBy(() -> lessonOrganizer.reorderLessons(unknownModuleId, List.of()))
			.isInstanceOf(CatalogItemNotFoundException.class)
			.hasMessageContaining(unknownModuleId.toString());
	}

}
