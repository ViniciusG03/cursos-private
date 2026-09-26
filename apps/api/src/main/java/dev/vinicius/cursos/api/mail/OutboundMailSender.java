package dev.vinicius.cursos.api.mail;

/**
 * Interface do projeto para envio de e-mail. O SMTP fica atrás dela ({@link SmtpOutboundMailSender})
 * e os testes usam uma fake nomeada, sem nenhuma chamada de rede.
 *
 * <p>Exemplo: {@code mailSender.send(new OutboundMail(email, subject, body));}
 */
public interface OutboundMailSender {

	/**
	 * Entrega a mensagem ou lança {@link MailDeliveryException}; nunca falha em silêncio.
	 */
	void send(OutboundMail mail);

}
