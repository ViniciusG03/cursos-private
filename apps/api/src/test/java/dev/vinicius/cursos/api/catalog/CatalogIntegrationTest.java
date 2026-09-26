package dev.vinicius.cursos.api.catalog;

import dev.vinicius.cursos.api.support.PostgresTestcontainersConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base dos testes de catálogo contra PostgreSQL real. Os testes não são transacionais de propósito:
 * cada serviço confirma a própria transação e as leituras abaixo vão direto ao banco via JDBC, o que
 * prova persistência após recarga e ausência de atualização parcial.
 */
@SpringBootTest
@Import(PostgresTestcontainersConfiguration.class)
public abstract class CatalogIntegrationTest {

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@BeforeEach
	protected void truncateCatalogTables() {
		jdbcTemplate.execute("TRUNCATE lessons, modules, courses");
	}

	protected List<UUID> readModuleIdsByPosition(UUID courseId) {
		return jdbcTemplate.queryForList("SELECT id FROM modules WHERE course_id = ? ORDER BY position", UUID.class,
				courseId);
	}

	protected List<Integer> readModulePositions(UUID courseId) {
		return jdbcTemplate.queryForList("SELECT position FROM modules WHERE course_id = ? ORDER BY position",
				Integer.class, courseId);
	}

	protected List<UUID> readLessonIdsByPosition(UUID moduleId) {
		return jdbcTemplate.queryForList("SELECT id FROM lessons WHERE module_id = ? ORDER BY position", UUID.class,
				moduleId);
	}

	protected List<Integer> readLessonPositions(UUID moduleId) {
		return jdbcTemplate.queryForList("SELECT position FROM lessons WHERE module_id = ? ORDER BY position",
				Integer.class, moduleId);
	}

	protected String readCourseStatus(UUID courseId) {
		return jdbcTemplate.queryForObject("SELECT status FROM courses WHERE id = ?", String.class, courseId);
	}

	protected UUID insertCourseRow(String title) {
		return jdbcTemplate.queryForObject("INSERT INTO courses (title) VALUES (?) RETURNING id", UUID.class, title);
	}

	protected UUID insertModuleRow(UUID courseId, String title, int position) {
		return jdbcTemplate.queryForObject(
				"INSERT INTO modules (course_id, title, position) VALUES (?, ?, ?) RETURNING id", UUID.class, courseId,
				title, position);
	}

	protected UUID insertLessonRow(UUID moduleId, String title, int position) {
		return jdbcTemplate.queryForObject(
				"INSERT INTO lessons (module_id, title, position) VALUES (?, ?, ?) RETURNING id", UUID.class, moduleId,
				title, position);
	}

}
