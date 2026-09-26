package dev.vinicius.cursos.api.security;

import dev.vinicius.cursos.api.account.repository.UserAccountRepository;
import dev.vinicius.cursos.api.security.attemptlimit.AttemptLimiter;
import dev.vinicius.cursos.api.security.attemptlimit.LoginAttemptLimitFilter;
import dev.vinicius.cursos.api.security.web.ProblemResponseWriter;
import dev.vinicius.cursos.api.security.web.SecurityResponseHandlers;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Cadeia de segurança da API. Nega por padrão: só saúde básica e os fluxos públicos da spec são
 * anônimos; {@code /api/admin/**} exige ADMIN; o restante declarado exige sessão. Autenticação por
 * sessão de servidor e cookie, com CSRF ativo em todo método que muda estado (inclusive login).
 */
@Configuration(proxyBeanMethods = false)
class WebSecurityConfiguration {

	static final String LOGIN_PATH = "/api/auth/login";

	static final String LOGOUT_PATH = "/api/auth/logout";

	@Bean
	SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, SecurityResponseHandlers responseHandlers,
			UserAccountRepository accountRepository, AttemptLimiter attemptLimiter,
			ProblemResponseWriter problemWriter) {
		http.authorizeHttpRequests(WebSecurityConfiguration::authorizeRoutes)
			// Token guardado na sessão e mascarado (XOR) a cada leitura; a SPA o obtém em GET /api/auth/csrf.
			.csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository())
				.csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler()))
			.httpBasic(AbstractHttpConfigurer::disable)
			.headers(headers -> headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)));
		configureLoginAndLogout(http, responseHandlers);
		configureSessionsAndErrors(http, responseHandlers);
		http.addFilterBefore(new LoginAttemptLimitFilter(attemptLimiter, problemWriter,
				PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, LOGIN_PATH)),
				UsernamePasswordAuthenticationFilter.class)
			// Antes do logout: sessão com senha antiga não consegue nem "sair", recebe 401 como qualquer outra rota.
			.addFilterBefore(new SessionCredentialGuardFilter(accountRepository), LogoutFilter.class);
		return http.build();
	}

	private static void authorizeRoutes(
			AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry routes) {
		// Erros de rotas já autorizadas (ex.: 400 num fluxo público) são renderizados em /error.
		routes.dispatcherTypeMatchers(DispatcherType.ERROR)
			.permitAll()
			.requestMatchers(HttpMethod.GET, "/actuator/health", "/api/auth/csrf")
			.permitAll()
			.requestMatchers(HttpMethod.POST, LOGIN_PATH, "/api/auth/invitations/accept",
					"/api/auth/password-resets/request", "/api/auth/password-resets/confirm")
			.permitAll()
			.requestMatchers("/api/admin/**")
			.hasRole("ADMIN")
			.requestMatchers(HttpMethod.GET, "/api/auth/me", "/api/courses", "/api/courses/*")
			.authenticated()
			.anyRequest()
			.denyAll();
	}

	// Login por formulário (application/x-www-form-urlencoded: email, password) e logout do próprio
	// Spring Security, que persistem e invalidam a sessão; as respostas são JSON, sem redirecionamento.
	private static void configureLoginAndLogout(HttpSecurity http, SecurityResponseHandlers responseHandlers) {
		http.formLogin(form -> form.loginPage(LOGIN_PATH)
			.loginProcessingUrl(LOGIN_PATH)
			.usernameParameter("email")
			.passwordParameter("password")
			.successHandler(responseHandlers)
			.failureHandler(responseHandlers))
			.logout(logout -> logout.logoutUrl(LOGOUT_PATH).logoutSuccessHandler(responseHandlers));
	}

	// Novo ID de sessão no login (contra fixação), nenhuma sessão criada só para lembrar a URL pedida
	// por um anônimo, e 401/403 em JSON no lugar do redirecionamento para a página de login.
	private static void configureSessionsAndErrors(HttpSecurity http, SecurityResponseHandlers responseHandlers) {
		http.sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
			.requestCache(cache -> cache.requestCache(new NullRequestCache()))
			.exceptionHandling(
					handling -> handling.authenticationEntryPoint(responseHandlers).accessDeniedHandler(responseHandlers));
	}

}
