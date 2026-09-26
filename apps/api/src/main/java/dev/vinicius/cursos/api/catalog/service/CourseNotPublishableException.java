package dev.vinicius.cursos.api.catalog.service;

/**
 * Curso estruturalmente incompleto: sem módulos ou com algum módulo sem aulas.
 *
 * <p>Exemplo: {@code throw new CourseNotPublishableException("course 1f... has no modules")}.
 */
public class CourseNotPublishableException extends RuntimeException {

	public CourseNotPublishableException(String message) {
		super(message);
	}

}
