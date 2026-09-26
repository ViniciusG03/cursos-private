package dev.vinicius.cursos.api.invitation.domain;

/**
 * Situação do envio do e-mail de convite, persistida em {@code invitations.delivery_status}.
 * {@code FAILED} é o estado observável que pede reenvio ao administrador.
 *
 * <p>Exemplo: {@code invitation.getDeliveryStatus() == InvitationDeliveryStatus.FAILED}.
 */
public enum InvitationDeliveryStatus {
	PENDING,
	SENT,
	FAILED
}
