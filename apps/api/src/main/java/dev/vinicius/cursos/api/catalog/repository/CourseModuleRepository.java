package dev.vinicius.cursos.api.catalog.repository;

import dev.vinicius.cursos.api.catalog.domain.CourseModule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistência de módulos, sempre lidos na ordem manual do curso. */
public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {

	/**
	 * Módulos do curso em {@code position} crescente.
	 *
	 * <p>Exemplo: {@code moduleRepository.findByCourseIdOrderByPositionAsc(courseId)}.
	 */
	List<CourseModule> findByCourseIdOrderByPositionAsc(UUID courseId);

	/**
	 * Maior posição ocupada no curso, ou 0 quando ele ainda não tem módulos.
	 *
	 * <p>Exemplo: {@code int next = moduleRepository.findLastPositionInCourse(courseId) + 1;}
	 */
	@Query("select coalesce(max(m.position), 0) from CourseModule m where m.course.id = :courseId")
	int findLastPositionInCourse(@Param("courseId") UUID courseId);

	/**
	 * Quantidade de módulos do curso, usada na regra de publicação.
	 *
	 * <p>Exemplo: {@code moduleRepository.countByCourseId(courseId) == 0}.
	 */
	long countByCourseId(UUID courseId);

	/**
	 * IDs dos módulos do curso que ainda não têm nenhuma aula, em ordem manual.
	 *
	 * <p>Exemplo: {@code moduleRepository.findIdsWithoutLessonsInCourse(courseId).isEmpty()}.
	 */
	@Query("""
			select m.id from CourseModule m
			where m.course.id = :courseId
			  and not exists (select 1 from Lesson l where l.module = m)
			order by m.position asc""")
	List<UUID> findIdsWithoutLessonsInCourse(@Param("courseId") UUID courseId);

}
