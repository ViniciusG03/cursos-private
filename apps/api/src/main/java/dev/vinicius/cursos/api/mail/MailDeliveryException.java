package dev.vinicius.cursos.api.mail;

/**
 * Falha ao entregar uma mensagem ao servidor de e-mail. A mensagem cita destinatário e causa, nunca
 * o corpo (que contém o link com token).
 *
 * <p>Exemplo: {@code throw new MailDeliveryException("SMTP delivery to a@b.com failed: ...", cause)}.
 */
public class MailDeliveryException extends RuntimeException {

	public MailDeliveryException(String message, Throwable cause) {
		super(message, cause);
	}

}
