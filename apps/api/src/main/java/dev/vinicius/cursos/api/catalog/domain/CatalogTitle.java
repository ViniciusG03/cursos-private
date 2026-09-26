package dev.vinicius.cursos.api.catalog.domain;

/**
 * Validação de títulos do catálogo antes de chegar ao banco, para que o erro cite o valor
 * recebido em vez de uma violação genérica de constraint.
 */
public final class CatalogTitle {

	private CatalogTitle() {}

	/**
	 * Exige um título não vazio (desconsiderando espaços) e dentro do limite da coluna.
	 *
	 * <p>Exemplo: {@code CatalogTitle.requireValid("Aula 1", 200, "lesson title")}.
	 *
	 * @return o título sem espaços nas pontas
	 */
	public static String requireValid(String title, int maxLength, String fieldName) {
		if (title == null || title.isBlank()) {
			throw new InvalidCatalogTitleException("%s must contain non-blank text, got: '%s'".formatted(fieldName, title));
		}
		String trimmedTitle = title.strip();
		if (trimmedTitle.length() > maxLength) {
			throw new InvalidCatalogTitleException(
					"%s must have at most %d characters, got %d: '%s'".formatted(fieldName, maxLength, trimmedTitle.length(),
							trimmedTitle));
		}
		return trimmedTitle;
	}

}
