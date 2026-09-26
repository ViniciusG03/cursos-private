package dev.vinicius.cursos.api.invitation.service;

/**
 * Convite para um e-mail que já tem conta ativa, ou emissão concorrente para o mesmo e-mail.
 *
 * <p>Exemplo: {@code throw new InvitationConflictException("email ana@x.com already has an account")}.
 */
public class InvitationConflictException extends RuntimeException {

	public InvitationConflictException(String message) {
		super(message);
	}

}
