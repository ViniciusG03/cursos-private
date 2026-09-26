package dev.vinicius.cursos.api.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vinicius.cursos.api.catalog.CatalogIntegrationTest;
import dev.vinicius.cursos.api.catalog.domain.CourseStatus;
import dev.vinicius.cursos.api.catalog.service.CourseOutline.LessonOutline;
import dev.vinicius.cursos.api.catalog.service.CourseOutline.ModuleOutline;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CourseOutlineReaderTest extends CatalogIntegrationTest {

	@Autowired
	private CourseOutlineReader outlineReader;

	@Test
	void readsModulesAndLessonsByPositionNotByInsertionOrTitle() {
		UUID courseId = insertCourseRow("Curso");
		// Inserção fora de ordem e títulos em ordem alfabética inversa à posição.
		UUID lastModuleId = insertModuleRow(courseId, "A - último", 2);
		UUID firstModuleId = insertModuleRow(courseId, "Z - primeiro", 1);
		insertLessonRow(firstModuleId, "A - terceira", 3);
		insertLessonRow(firstModuleId, "Z - primeira", 1);
		insertLessonRow(firstModuleId, "M - segunda", 2);

		CourseOutline outline = outlineReader.readOutline(courseId);

		assertThat(outline.modules()).extracting(ModuleOutline::id).containsExactly(firstModuleId, lastModuleId);
		assertThat(outline.modules().getFirst().lessons()).extracting(LessonOutline::title)
			.containsExactly("Z - primeira", "M - segunda", "A - terceira");
		assertThat(outline.modules().getLast().lessons()).isEmpty();
	}

	@Test
	void readsDraftWithoutModules() {
		UUID courseId = insertCourseRow("Rascunho vazio");

		CourseOutline outline = outlineReader.readOutline(courseId);

		assertThat(outline.status()).isEqualTo(CourseStatus.DRAFT);
		assertThat(outline.title()).isEqualTo("Rascunho vazio");
		assertThat(outline.modules()).isEmpty();
	}

	@Test
	void rejectsUnknownCourse() {
		UUID unknownCourseId = UUID.randomUUID();

		assertThatThrownBy(() -> outlineReader.readOutline(unknownCourseId))
			.isInstanceOf(CatalogItemNotFoundException.class)
			.hasMessageContaining(unknownCourseId.toString());
	}

}
