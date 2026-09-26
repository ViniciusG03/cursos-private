package dev.vinicius.cursos.api.passwordreset.domain;

import dev.vinicius.cursos.api.onetimetoken.InvalidOneTimeTokenException;
import dev.vinicius.cursos.api.onetimetoken.OneTimeTokenState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Token de recuperação de acesso, mapeado sobre {@code password_reset_tokens} (V6). Só guarda o hash. */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

	public static final Duration VALIDITY = Duration.ofMinutes(30);

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected PasswordResetToken() {}

	private PasswordResetToken(UUID userId, String tokenHash, Instant now) {
		this.userId = userId;
		this.tokenHash = tokenHash;
		this.createdAt = now;
		this.expiresAt = now.plus(VALIDITY);
	}

	/**
	 * Emite um token válido por {@link #VALIDITY} a partir de {@code now}.
	 *
	 * <p>Exemplo: {@code PasswordResetToken.issue(userId, OneTimeTokenDigest.sha256Hex(raw), now)}.
	 */
	public static PasswordResetToken issue(UUID userId, String tokenHash, Instant now) {
		return new PasswordResetToken(userId, tokenHash, now);
	}

	/**
	 * Marca o token como usado ou rejeita com a exceção genérica se ele já não é utilizável.
	 *
	 * <p>Exemplo: {@code resetToken.consume(clock.instant());}
	 */
	public void consume(Instant now) {
		if (!OneTimeTokenState.isUsable(consumedAt, revokedAt, expiresAt, now)) {
			throw new InvalidOneTimeTokenException();
		}
		this.consumedAt = now;
	}

	/**
	 * Revoga um token que ainda não foi usado (ex.: o e-mail com o link não saiu). Já usado ou revogado
	 * permanece como está.
	 *
	 * <p>Exemplo: {@code resetToken.revokeIfOpen(clock.instant());}
	 */
	public void revokeIfOpen(Instant now) {
		if (consumedAt == null && revokedAt == null) {
			this.revokedAt = now;
		}
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getConsumedAt() {
		return consumedAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

}
