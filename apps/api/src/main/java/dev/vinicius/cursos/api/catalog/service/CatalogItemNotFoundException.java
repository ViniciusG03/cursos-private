package dev.vinicius.cursos.api.catalog.service;

import java.util.UUID;

/**
 * Curso ou módulo pai inexistente em uma operação de organização do catálogo.
 *
 * <p>Exemplo: {@code throw new CatalogItemNotFoundException("course", courseId)}.
 */
public class CatalogItemNotFoundException extends RuntimeException {

	public CatalogItemNotFoundException(String itemKind, UUID itemId) {
		super("%s not found: expected an existing %s id, got %s".formatted(itemKind, itemKind, itemId));
	}

}
