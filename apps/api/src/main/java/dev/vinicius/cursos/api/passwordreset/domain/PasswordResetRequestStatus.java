package dev.vinicius.cursos.api.passwordreset.domain;

/**
 * Situação de um pedido de recuperação na fila. {@code FAILED} é o estado observável de quem esgotou as
 * tentativas de envio; o usuário resolve pedindo um novo link, que cria outro pedido.
 *
 * <p>Exemplo: {@code request.getStatus() == PasswordResetRequestStatus.PENDING}.
 */
public enum PasswordResetRequestStatus {
	PENDING,
	SENT,
	NO_ACCOUNT,
	FAILED
}
