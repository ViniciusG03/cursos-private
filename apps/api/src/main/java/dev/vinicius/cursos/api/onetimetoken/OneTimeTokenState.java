package dev.vinicius.cursos.api.onetimetoken;

import java.time.Instant;

/**
 * Regra de validade compartilhada por convites e tokens de recuperação: usável somente se não foi
 * consumido nem revogado e se {@code now < expires_at} (instantes UTC; o limite exato já é expirado).
 */
public final class OneTimeTokenState {

	private OneTimeTokenState() {}

	/**
	 * Indica se o token pode ser consumido no instante informado.
	 *
	 * <p>Exemplo: {@code OneTimeTokenState.isUsable(consumedAt, revokedAt, expiresAt, clock.instant())}.
	 */
	public static boolean isUsable(Instant consumedAt, Instant revokedAt, Instant expiresAt, Instant now) {
		return consumedAt == null && revokedAt == null && now.isBefore(expiresAt);
	}

}
