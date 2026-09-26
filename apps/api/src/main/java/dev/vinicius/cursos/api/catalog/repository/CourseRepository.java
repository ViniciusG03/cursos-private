package dev.vinicius.cursos.api.catalog.repository;

import dev.vinicius.cursos.api.catalog.domain.Course;
import dev.vinicius.cursos.api.catalog.domain.CourseStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistência de cursos.
 *
 * <p>Exemplo: {@code courseRepository.findById(courseId)}.
 */
public interface CourseRepository extends JpaRepository<Course, UUID> {

	/**
	 * Todos os cursos, rascunhos inclusive, por título; usado na visão do administrador.
	 *
	 * <p>Exemplo: {@code courseRepository.findAllByOrderByTitleAsc()}.
	 */
	List<Course> findAllByOrderByTitleAsc();

	/**
	 * Cursos em um estado, por título; usado para mostrar somente publicados aos membros.
	 *
	 * <p>Exemplo: {@code courseRepository.findByStatusOrderByTitleAsc(CourseStatus.PUBLISHED)}.
	 */
	List<Course> findByStatusOrderByTitleAsc(CourseStatus status);

}
