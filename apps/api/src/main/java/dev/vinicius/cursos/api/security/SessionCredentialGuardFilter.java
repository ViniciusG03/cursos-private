package dev.vinicius.cursos.api.security;

import dev.vinicius.cursos.api.account.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Revalida a sessão a cada requisição autenticada: relê a conta no banco, derruba a sessão se a
 * {@code credential_version} mudou (senha trocada) ou a conta sumiu, e usa o papel atual do banco em
 * vez do papel gravado na sessão. A troca vale só para esta requisição; a sessão mantém a versão do login.
 */
final class SessionCredentialGuardFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(SessionCredentialGuardFilter.class);

	private final UserAccountRepository accountRepository;

	private final SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder
		.getContextHolderStrategy();

	SessionCredentialGuardFilter(UserAccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = contextHolderStrategy.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof LibraryUserPrincipal sessionPrincipal) {
			revalidate(sessionPrincipal, request);
		}
		filterChain.doFilter(request, response);
	}

	private void revalidate(LibraryUserPrincipal sessionPrincipal, HttpServletRequest request) {
		Optional<LibraryUserPrincipal> currentPrincipal = accountRepository.findById(sessionPrincipal.userId())
			.filter(account -> account.getCredentialVersion() == sessionPrincipal.credentialVersion())
			.map(LibraryUserPrincipal::authenticated);
		if (currentPrincipal.isEmpty()) {
			discardStaleSession(sessionPrincipal, request);
			return;
		}
		SecurityContext refreshedContext = contextHolderStrategy.createEmptyContext();
		refreshedContext.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(currentPrincipal.get(),
				null, currentPrincipal.get().getAuthorities()));
		contextHolderStrategy.setContext(refreshedContext);
	}

	private void discardStaleSession(LibraryUserPrincipal sessionPrincipal, HttpServletRequest request) {
		HttpSession staleSession = request.getSession(false);
		if (staleSession != null) {
			staleSession.invalidate();
		}
		contextHolderStrategy.clearContext();
		log.atInfo()
			.addKeyValue("userId", sessionPrincipal.userId())
			.addKeyValue("sessionCredentialVersion", sessionPrincipal.credentialVersion())
			.log("stale session discarded");
	}

}
