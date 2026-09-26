package dev.vinicius.cursos.api.security.attemptlimit;

import dev.vinicius.cursos.api.security.web.ClientAddress;
import dev.vinicius.cursos.api.security.web.ProblemResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Conta tentativas de login antes do filtro de autenticação do Spring Security, porque o login não
 * passa por controller. Os outros fluxos públicos chamam o {@link AttemptLimiter} no controller.
 */
public final class LoginAttemptLimitFilter extends OncePerRequestFilter {

	private final AttemptLimiter attemptLimiter;

	private final ProblemResponseWriter problemWriter;

	private final RequestMatcher loginRequestMatcher;

	public LoginAttemptLimitFilter(AttemptLimiter attemptLimiter, ProblemResponseWriter problemWriter,
			RequestMatcher loginRequestMatcher) {
		this.attemptLimiter = attemptLimiter;
		this.problemWriter = problemWriter;
		this.loginRequestMatcher = loginRequestMatcher;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !loginRequestMatcher.matches(request);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			attemptLimiter.recordAttempt(AttemptScope.LOGIN, ClientAddress.of(request), request.getParameter("email"));
		}
		catch (TooManyAttemptsException tooManyAttempts) {
			response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(tooManyAttempts.retryAfterSeconds()));
			problemWriter.writeProblem(response, HttpStatus.TOO_MANY_REQUESTS, tooManyAttempts.getMessage());
			return;
		}
		filterChain.doFilter(request, response);
	}

}
