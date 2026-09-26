package dev.vinicius.cursos.api.invitation.service;

import dev.vinicius.cursos.api.invitation.domain.Invitation;
import dev.vinicius.cursos.api.invitation.domain.InvitationDeliveryStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Situação de um convite para o administrador, sem token nem hash.
 *
 * <p>Exemplo: {@code InvitationView.of(invitation).deliveryStatus()}.
 */
public record InvitationView(UUID id, String email, Instant createdAt, Instant expiresAt,
		InvitationDeliveryStatus deliveryStatus, Instant deliveryAttemptedAt, Instant deliveredAt) {

	/**
	 * Copia os campos públicos da entidade.
	 *
	 * <p>Exemplo: {@code InvitationView view = InvitationView.of(invitation);}
	 */
	public static InvitationView of(Invitation invitation) {
		return new InvitationView(invitation.getId(), invitation.getEmail().value(), invitation.getCreatedAt(),
				invitation.getExpiresAt(), invitation.getDeliveryStatus(), invitation.getDeliveryAttemptedAt(),
				invitation.getDeliveredAt());
	}

}
