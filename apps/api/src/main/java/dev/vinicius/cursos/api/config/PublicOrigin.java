package dev.vinicius.cursos.api.config;

import java.net.URI;
import java.util.Locale;

/**
 * Origem pública do frontend ({@code esquema://host[:porta]}) usada nos links de convite e recuperação.
 * Os links carregam tokens, então fora do perfil local só HTTPS é aceito: um link HTTP exporia o token
 * na rede. Caminho, query, fragmento e credenciais são rejeitados porque uma origem não os tem e eles
 * mudariam para onde o token vai.
 *
 * <p>Exemplo: {@code PublicOrigin.parse(URI.create("https://cursos.example.com"), false).value()}.
 */
public record PublicOrigin(String value) {

	private static final String SETTING = "library.access.public-base-url (LIBRARY_PUBLIC_BASE_URL)";

	/**
	 * Valida a URL configurada e devolve a origem sem barra final. {@code allowPlainHttp} só é verdadeiro
	 * no perfil {@code local}.
	 *
	 * <p>Exemplo: {@code PublicOrigin.parse(URI.create("http://localhost:5173"), true)}.
	 */
	public static PublicOrigin parse(URI configuredUrl, boolean allowPlainHttp) {
		if (configuredUrl == null) {
			throw invalid("an absolute URL", null);
		}
		requireAllowedScheme(configuredUrl, allowPlainHttp);
		requireOriginOnly(configuredUrl);
		String scheme = configuredUrl.getScheme().toLowerCase(Locale.ROOT);
		String port = configuredUrl.getPort() == -1 ? "" : ":" + configuredUrl.getPort();
		return new PublicOrigin(scheme + "://" + configuredUrl.getHost().toLowerCase(Locale.ROOT) + port);
	}

	/**
	 * Link para um caminho do frontend com o token no fragmento (não vai ao servidor nem ao Referer).
	 *
	 * <p>Exemplo: {@code origin.linkWithToken("/convites/aceitar", rawToken)}.
	 */
	public String linkWithToken(String path, String rawToken) {
		return value + path + "#token=" + rawToken;
	}

	private static void requireAllowedScheme(URI configuredUrl, boolean allowPlainHttp) {
		String scheme = configuredUrl.getScheme() == null ? "" : configuredUrl.getScheme().toLowerCase(Locale.ROOT);
		if (scheme.equals("https") || (allowPlainHttp && scheme.equals("http"))) {
			return;
		}
		throw invalid(allowPlainHttp ? "an http(s) URL" : "an https URL (http is accepted only in the local profile)",
				configuredUrl);
	}

	private static void requireOriginOnly(URI configuredUrl) {
		String path = configuredUrl.getRawPath();
		boolean hasHost = configuredUrl.getHost() != null && !configuredUrl.getHost().isBlank();
		boolean onlyOrigin = (path == null || path.isEmpty() || path.equals("/")) && configuredUrl.getRawQuery() == null
				&& configuredUrl.getRawFragment() == null && configuredUrl.getRawUserInfo() == null;
		if (!hasHost || !onlyOrigin) {
			throw invalid("an origin with host and no user info, path, query or fragment", configuredUrl);
		}
	}

	private static IllegalArgumentException invalid(String expected, URI configuredUrl) {
		return new IllegalArgumentException("%s must be %s, like https://cursos.example.com, got: '%s'"
			.formatted(SETTING, expected, configuredUrl));
	}

}
