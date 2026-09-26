package dev.vinicius.cursos.api.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** Restrições de V3 verificadas diretamente no PostgreSQL, sem passar pelas regras da aplicação. */
class LessonsSchemaTest extends CatalogIntegrationTest {

	private UUID moduleId;

	@BeforeEach
	void createParentModule() {
		moduleId = insertModuleRow(insertCourseRow("Curso"), "Módulo", 1);
	}

	@Test
	void flywayAppliesV1V2AndV3() {
		List<String> appliedVersions = jdbcTemplate.queryForList(
				"SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

		// containsSubsequence e não containsExactly: migrações futuras (V4+) não devem quebrar este teste.
		assertThat(appliedVersions).containsSubsequence("1", "2", "3");
	}

	@Test
	void databaseGeneratesLessonIdByDefault() {
		assertThat(insertLessonRow(moduleId, "Aula", 1)).isNotNull();
	}

	@Test
	void rejectsLessonWithoutExistingModule() {
		assertThatThrownBy(() -> insertLessonRow(UUID.randomUUID(), "Aula órfã", 1))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("fk_lessons_module");
	}

	@Test
	void rejectsZeroPosition() {
		assertThatThrownBy(() -> insertLessonRow(moduleId, "Aula", 0))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ck_lessons_position_positive");
	}

	@Test
	void rejectsNegativePosition() {
		assertThatThrownBy(() -> insertLessonRow(moduleId, "Aula", -1))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ck_lessons_position_positive");
	}

	@Test
	void rejectsTwoLessonsInSamePositionOfSameModule() {
		insertLessonRow(moduleId, "Primeira", 1);

		assertThatThrownBy(() -> insertLessonRow(moduleId, "Segunda", 1))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("uq_lessons_module_position");
	}

	@Test
	void allowsSamePositionInDistinctModules() {
		UUID otherModuleId = insertModuleRow(insertCourseRow("Outro curso"), "Módulo", 1);
		insertLessonRow(moduleId, "Aula", 1);
		insertLessonRow(otherModuleId, "Aula", 1);

		assertThat(readLessonPositions(moduleId)).containsExactly(1);
		assertThat(readLessonPositions(otherModuleId)).containsExactly(1);
	}

	@Test
	void allowsRepeatedLessonTitlesInSameModule() {
		insertLessonRow(moduleId, "Exercícios", 1);
		insertLessonRow(moduleId, "Exercícios", 2);

		assertThat(readLessonPositions(moduleId)).containsExactly(1, 2);
	}

	@Test
	void doesNotCascadeModuleDeletionToLessons() {
		insertLessonRow(moduleId, "Aula", 1);

		assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM modules WHERE id = ?", moduleId))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("fk_lessons_module");
	}

}
