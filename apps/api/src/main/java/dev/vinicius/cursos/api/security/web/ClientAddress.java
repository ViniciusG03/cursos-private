package dev.vinicius.cursos.api.security.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Origem da requisição para o limite de tentativas. Atrás do Nginx, o Tomcat já substitui o endereço
 * remoto pelo {@code X-Forwarded-For} de proxies internos ({@code server.forward-headers-strategy=native}).
 */
public final class ClientAddress {

	private ClientAddress() {}

	/**
	 * Endereço do cliente como o contêiner o resolveu.
	 *
	 * <p>Exemplo: {@code attemptLimiter.recordAttempt(scope, ClientAddress.of(request), null);}
	 */
	public static String of(HttpServletRequest request) {
		return request.getRemoteAddr();
	}

}
