package dev.vinicius.cursos.api.security.web;

import dev.vinicius.cursos.api.security.LibraryUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas de sessão que passam pelo Spring MVC. Login e logout não estão aqui: são tratados pelos filtros
 * do Spring Security em {@code POST /api/auth/login} e {@code POST /api/auth/logout}.
 */
@RestController
@RequestMapping("/api/auth")
class AuthSessionController {

	// Sessões anônimas só carregam o token CSRF; expiram cedo para que um loop em /csrf não acumule
	// sessões por 8 horas na memória do Tomcat. O login devolve o timeout normal (SecurityResponseHandlers).
	static final Duration ANONYMOUS_SESSION_TIMEOUT = Duration.ofMinutes(15);

	/**
	 * Entrega o token CSRF (mascarado a cada chamada) para a SPA enviar no cabeçalho indicado. O token muda
	 * no login e no logout, então a SPA deve chamar esta rota de novo depois deles.
	 *
	 * <p>Exemplo: {@code GET /api/auth/csrf -> {"headerName":"X-CSRF-TOKEN", ...}}.
	 */
	@GetMapping("/csrf")
	CsrfTokenView csrf(CsrfToken csrfToken, @AuthenticationPrincipal LibraryUserPrincipal principal,
			HttpServletRequest request) {
		CsrfTokenView view = new CsrfTokenView(csrfToken.getHeaderName(), csrfToken.getParameterName(),
				csrfToken.getToken());
		HttpSession session = request.getSession(false);
		if (principal == null && session != null) {
			session.setMaxInactiveInterval((int) ANONYMOUS_SESSION_TIMEOUT.toSeconds());
		}
		return view;
	}

	/**
	 * Usuário da sessão atual, com o papel lido do banco nesta requisição.
	 *
	 * <p>Exemplo: {@code GET /api/auth/me -> {"id":"...","email":"ana@x.com","role":"MEMBER"}}.
	 */
	@GetMapping("/me")
	CurrentUserView me(@AuthenticationPrincipal LibraryUserPrincipal principal) {
		return CurrentUserView.of(principal);
	}

	/** Token CSRF e onde enviá-lo. */
	record CsrfTokenView(String headerName, String parameterName, String token) {}

}
