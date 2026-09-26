package dev.vinicius.cursos.api.catalog.service;

/**
 * Operação que deixaria um curso publicado estruturalmente incompleto, como acrescentar um módulo
 * ainda sem aulas.
 *
 * <p>Exemplo: {@code throw new PublishedCourseModificationException("course 1f... is PUBLISHED")}.
 */
public class PublishedCourseModificationException extends RuntimeException {

	public PublishedCourseModificationException(String message) {
		super(message);
	}

}
