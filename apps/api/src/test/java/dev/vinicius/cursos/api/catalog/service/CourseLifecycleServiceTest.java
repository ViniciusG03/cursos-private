package dev.vinicius.cursos.api.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vinicius.cursos.api.catalog.CatalogIntegrationTest;
import dev.vinicius.cursos.api.catalog.domain.InvalidCatalogTitleException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CourseLifecycleServiceTest extends CatalogIntegrationTest {

	@Autowired
	private CourseLifecycleService lifecycleService;

	@Autowired
	private CourseModuleOrganizer moduleOrganizer;

	@Autowired
	private LessonOrganizer lessonOrganizer;

	@Test
	void createsDraftWithoutDescription() {
		UUID courseId = lifecycleService.createDraft("Java moderno", null);

		assertThat(readCourseStatus(courseId)).isEqualTo("DRAFT");
		assertThat(jdbcTemplate.queryForObject("SELECT description FROM courses WHERE id = ?", String.class, courseId))
			.isNull();
	}

	@Test
	void allowsRepeatedCourseTitles() {
		UUID firstId = lifecycleService.createDraft("Java", null);
		UUID secondId = lifecycleService.createDraft("Java", null);

		assertThat(firstId).isNotEqualTo(secondId);
	}

	@Test
	void rejectsBlankCourseTitle() {
		assertThatThrownBy(() -> lifecycleService.createDraft("   ", null))
			.isInstanceOf(InvalidCatalogTitleException.class)
			.hasMessageContaining("course title");
	}

	@Test
	void courseWithoutModulesStaysDraftAndCannotBePublished() {
		UUID courseId = lifecycleService.createDraft("Vazio", null);

		assertThatThrownBy(() -> lifecycleService.publish(courseId))
			.isInstanceOf(CourseNotPublishableException.class)
			.hasMessageContaining("at least one module, got 0")
			.hasMessageContaining(courseId.toString());
		assertThat(readCourseStatus(courseId)).isEqualTo("DRAFT");
	}

	@Test
	void courseWithModuleWithoutLessonsStaysDraftAndCannotBePublished() {
		UUID courseId = lifecycleService.createDraft("Incompleto", null);
		UUID filledModuleId = moduleOrganizer.appendModule(courseId, "Com aula");
		lessonOrganizer.appendLesson(filledModuleId, "Aula 1");
		UUID emptyModuleId = moduleOrganizer.appendModule(courseId, "Sem aula");

		assertThatThrownBy(() -> lifecycleService.publish(courseId))
			.isInstanceOf(CourseNotPublishableException.class)
			.hasMessageContaining("modules without lessons: [" + emptyModuleId + "]");
		assertThat(readCourseStatus(courseId)).isEqualTo("DRAFT");
	}

	@Test
	void completeCourseCanBePublished() {
		UUID courseId = lifecycleService.createDraft("Completo", "Descrição");
		UUID firstModuleId = moduleOrganizer.appendModule(courseId, "Módulo 1");
		UUID secondModuleId = moduleOrganizer.appendModule(courseId, "Módulo 2");
		lessonOrganizer.appendLesson(firstModuleId, "Aula 1.1");
		lessonOrganizer.appendLesson(secondModuleId, "Aula 2.1");

		lifecycleService.publish(courseId);

		assertThat(readCourseStatus(courseId)).isEqualTo("PUBLISHED");
	}

	@Test
	void addingLessonsDoesNotPublishAutomatically() {
		UUID courseId = lifecycleService.createDraft("Completo mas rascunho", null);
		lessonOrganizer.appendLesson(moduleOrganizer.appendModule(courseId, "Módulo"), "Aula");

		assertThat(readCourseStatus(courseId)).isEqualTo("DRAFT");
	}

	@Test
	void rejectsPublishingUnknownCourse() {
		UUID unknownCourseId = UUID.randomUUID();

		assertThatThrownBy(() -> lifecycleService.publish(unknownCourseId))
			.isInstanceOf(CatalogItemNotFoundException.class)
			.hasMessageContaining(unknownCourseId.toString());
	}

}
