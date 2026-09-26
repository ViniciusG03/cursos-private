package dev.vinicius.cursos.api.security.attemptlimit;

import java.time.Duration;

/**
 * Limite de tentativas atingido; vira HTTP 429 com {@code Retry-After}.
 *
 * <p>Exemplo: {@code throw new TooManyAttemptsException(AttemptScope.LOGIN, 10, Duration.ofMinutes(3))}.
 */
public class TooManyAttemptsException extends RuntimeException {

	private final Duration retryAfter;

	public TooManyAttemptsException(AttemptScope scope, int maxAttempts, Duration retryAfter) {
		super("too many %s attempts: expected at most %d per window, retry after %d seconds".formatted(scope,
				maxAttempts, retryAfter.toSeconds()));
		this.retryAfter = retryAfter;
	}

	/**
	 * Segundos para o cabeçalho {@code Retry-After}, arredondados para cima e nunca menores que 1.
	 *
	 * <p>Exemplo: {@code response.setHeader("Retry-After", String.valueOf(e.retryAfterSeconds()));}
	 */
	public long retryAfterSeconds() {
		long wholeSeconds = retryAfter.toSeconds() + (retryAfter.toNanosPart() > 0 ? 1 : 0);
		return Math.max(1, wholeSeconds);
	}

}
