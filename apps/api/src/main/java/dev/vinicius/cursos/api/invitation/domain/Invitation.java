package dev.vinicius.cursos.api.invitation.domain;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.onetimetoken.InvalidOneTimeTokenException;
import dev.vinicius.cursos.api.onetimetoken.OneTimeTokenState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Convite de uso único, mapeado sobre {@code invitations} (V5). Guarda apenas o hash do token; o
 * {@code MEMBER} não existe até o aceite.
 */
@Entity
@Table(name = "invitations")
public class Invitation {

	public static final Duration VALIDITY = Duration.ofHours(72);

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "email", nullable = false, length = AccountEmail.MAX_LENGTH)
	private String email;

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

	// Só o ID: o convite não precisa navegar até a conta, e o papel ADMIN é garantido pela rota.
	@Column(name = "created_by_user_id", nullable = false)
	private UUID createdByUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "delivery_status", nullable = false, length = 20)
	private InvitationDeliveryStatus deliveryStatus;

	@Column(name = "delivery_attempted_at")
	private Instant deliveryAttemptedAt;

	@Column(name = "delivered_at")
	private Instant deliveredAt;

	protected Invitation() {}

	private Invitation(AccountEmail email, String tokenHash, UUID createdByUserId, Instant now) {
		this.email = email.value();
		this.tokenHash = tokenHash;
		this.createdByUserId = createdByUserId;
		this.createdAt = now;
		this.expiresAt = now.plus(VALIDITY);
		this.deliveryStatus = InvitationDeliveryStatus.PENDING;
	}

	/**
	 * Emite um convite válido por {@link #VALIDITY} a partir de {@code now}, ainda sem envio.
	 *
	 * <p>Exemplo: {@code Invitation.issue(email, OneTimeTokenDigest.sha256Hex(raw), adminId, now)}.
	 */
	public static Invitation issue(AccountEmail email, String tokenHash, UUID createdByUserId, Instant now) {
		return new Invitation(email, tokenHash, createdByUserId, now);
	}

	/**
	 * Marca o convite como usado. Rejeita com a mesma exceção genérica se ele já foi usado, revogado
	 * ou expirou, para não revelar qual foi o caso.
	 *
	 * <p>Exemplo: {@code invitation.consume(clock.instant());}
	 */
	public void consume(Instant now) {
		if (!OneTimeTokenState.isUsable(consumedAt, revokedAt, expiresAt, now)) {
			throw new InvalidOneTimeTokenException();
		}
		this.consumedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public AccountEmail getEmail() {
		return new AccountEmail(email);
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

	public UUID getCreatedByUserId() {
		return createdByUserId;
	}

	public InvitationDeliveryStatus getDeliveryStatus() {
		return deliveryStatus;
	}

	public Instant getDeliveryAttemptedAt() {
		return deliveryAttemptedAt;
	}

	public Instant getDeliveredAt() {
		return deliveredAt;
	}

}
