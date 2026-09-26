package dev.vinicius.cursos.api.catalog.repository;

import dev.vinicius.cursos.api.catalog.domain.Course;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistência de cursos.
 *
 * <p>Exemplo: {@code courseRepository.findById(courseId)}.
 */
public interface CourseRepository extends JpaRepository<Course, UUID> {}
