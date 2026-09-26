package dev.vinicius.cursos.api.catalog.service;

/**
 * Lista de reordenação que não é uma permutação completa dos filhos do pai.
 *
 * <p>Exemplo: {@code throw new InvalidReorderException("duplicated ids: [..]")}.
 */
public class InvalidReorderException extends IllegalArgumentException {

	public InvalidReorderException(String message) {
		super(message);
	}

}
