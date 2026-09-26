package dev.vinicius.cursos.api.catalog.domain;

/**
 * Estado editorial de um curso, persistido como texto na coluna {@code courses.status}.
 *
 * <p>Exemplo: {@code course.getStatus() == CourseStatus.DRAFT}.
 */
public enum CourseStatus {
	DRAFT,
	PUBLISHED
}
