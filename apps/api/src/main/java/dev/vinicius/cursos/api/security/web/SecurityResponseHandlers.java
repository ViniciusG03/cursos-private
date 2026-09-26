package dev.vinicius.cursos.api.security.web;

import dev.vinicius.cursos.api.security.LibraryUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Respostas JSON do Spring Security para uma API consumida por SPA: nada de redirecionamentos para
 * página de login. 401 para anônimo, 403 para autenticado sem permissão (ou CSRF ausente/inválido) e
 * erro de login genérico, igual para e-mail inexistente e senha errada.
 */
@Component
public class SecurityResponseHandlers implements AuthenticationEntryPoint, AccessDeniedHandler,
		AuthenticationSuccessHandler, AuthenticationFailureHandler, LogoutSuccessHandler {

	private final ProblemResponseWriter problemWriter;

	private final Duration authenticatedSessionTimeout;

	SecurityResponseHandlers(ProblemResponseWriter problemWriter,
			@Value("${server.servlet.session.timeout}") Duration authenticatedSessionTimeout) {
		this.problemWriter = problemWriter;
		this.authenticatedSessionTimeout = authenticatedSessionTimeout;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		problemWriter.writeProblem(response, HttpStatus.UNAUTHORIZED, "authentication required");
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		problemWriter.writeProblem(response, HttpStatus.FORBIDDEN,
				"access denied: expected a permitted role and a valid CSRF token");
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		LibraryUserPrincipal principal = (LibraryUserPrincipal) authentication.getPrincipal();
		// A sessão nasceu anônima (timeout curto em GET /api/auth/csrf) e mantém o objeto após a troca de ID.
		request.getSession().setMaxInactiveInterval((int) authenticatedSessionTimeout.toSeconds());
		problemWriter.writeJson(response, HttpStatus.OK, CurrentUserView.of(principal));
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		problemWriter.writeProblem(response, HttpStatus.UNAUTHORIZED, "invalid email or password");
	}

	// O LogoutFilter roda antes da autorização; sem sessão autenticada o logout responde 401, como a spec pede.
	@Override
	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		if (authentication == null) {
			problemWriter.writeProblem(response, HttpStatus.UNAUTHORIZED, "authentication required");
			return;
		}
		response.setStatus(HttpStatus.NO_CONTENT.value());
	}

}
