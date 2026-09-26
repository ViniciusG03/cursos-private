package dev.vinicius.cursos.api.security.attemptlimit;

/**
 * Fluxo público cujas tentativas são contadas. Cada escopo tem contadores próprios, para que muitos
 * logins não bloqueiem a recuperação de acesso e vice-versa.
 *
 * <p>Exemplo: {@code attemptLimiter.recordAttempt(AttemptScope.LOGIN, clientAddress, email);}
 */
public enum AttemptScope {
	LOGIN,
	PASSWORD_RESET_REQUEST,
	PASSWORD_RESET_CONFIRM,
	INVITATION_ACCEPT
}
