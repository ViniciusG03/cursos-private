package dev.vinicius.cursos.api.mail;

/**
 * Resultado de uma tentativa de envio: entregue, ou a mensagem de falha (destinatário e erro SMTP, sem o
 * corpo) para quem precisa registrá-la.
 *
 * <p>Exemplo: {@code if (!result.delivered()) request.recordFailedAttempt(now, result.failure(), ...);}
 */
public record MailDeliveryResult(boolean delivered, String failure) {

	static MailDeliveryResult success() {
		return new MailDeliveryResult(true, null);
	}

	static MailDeliveryResult failedWith(String failure) {
		return new MailDeliveryResult(false, failure);
	}

}
