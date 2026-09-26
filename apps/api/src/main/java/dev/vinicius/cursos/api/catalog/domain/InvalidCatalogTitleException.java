package dev.vinicius.cursos.api.catalog.domain;

/**
 * Título de curso, módulo ou aula vazio, só com espaços ou maior que a coluna.
 *
 * <p>Exemplo: {@code throw new InvalidCatalogTitleException("course title must ...")}.
 */
public class InvalidCatalogTitleException extends IllegalArgumentException {

	public InvalidCatalogTitleException(String message) {
		super(message);
	}

}
