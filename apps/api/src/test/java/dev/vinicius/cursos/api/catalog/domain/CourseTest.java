package dev.vinicius.cursos.api.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CourseTest {

	@Test
	void draftCourseStartsAsDraft() {
		Course course = Course.draft("Java moderno", null);

		assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
		assertThat(course.getDescription()).isNull();
	}

	@Test
	void markPublishedChangesStatus() {
		Course course = Course.draft("Java moderno", "Descrição");
		course.markPublished();

		assertThat(course.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
	}

	@Test
	void rejectsTitleLongerThanCoursesColumn() {
		assertThatThrownBy(() -> Course.draft("a".repeat(251), null)).isInstanceOf(InvalidCatalogTitleException.class)
			.hasMessageContaining("at most 250 characters");
	}

}
