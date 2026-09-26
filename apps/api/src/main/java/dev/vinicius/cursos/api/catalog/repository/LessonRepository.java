package dev.vinicius.cursos.api.catalog.repository;

import dev.vinicius.cursos.api.catalog.domain.Lesson;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistência de aulas, sempre lidas na ordem manual do módulo. */
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

	/**
	 * Aulas do módulo em {@code position} crescente.
	 *
	 * <p>Exemplo: {@code lessonRepository.findByModuleIdOrderByPositionAsc(moduleId)}.
	 */
	List<Lesson> findByModuleIdOrderByPositionAsc(UUID moduleId);

	/**
	 * Maior posição ocupada no módulo, ou 0 quando ele ainda não tem aulas.
	 *
	 * <p>Exemplo: {@code int next = lessonRepository.findLastPositionInModule(moduleId) + 1;}
	 */
	@Query("select coalesce(max(l.position), 0) from Lesson l where l.module.id = :moduleId")
	int findLastPositionInModule(@Param("moduleId") UUID moduleId);

}
