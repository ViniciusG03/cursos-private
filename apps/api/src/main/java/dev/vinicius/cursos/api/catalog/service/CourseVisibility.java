package dev.vinicius.cursos.api.catalog.service;

/**
 * O que o leitor do catálogo pode ver. Decidido pelo papel da sessão, nunca por parâmetro do cliente.
 *
 * <p>Exemplo: {@code browser.listCourses(CourseVisibility.PUBLISHED_ONLY)}.
 */
public enum CourseVisibility {
	PUBLISHED_ONLY,
	INCLUDING_DRAFTS
}
