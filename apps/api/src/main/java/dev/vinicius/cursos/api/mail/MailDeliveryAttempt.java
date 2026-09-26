package dev.vinicius.cursos.api.mail;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tenta uma entrega e transforma o resultado em um {@link MailDeliveryResult} registrado em log estruturado. Quem chama
 * decide o que fazer com a falha (ex.: marcar o convite como {@code FAILED}); ela nunca é engolida.
 */
@Component
public class MailDeliveryAttempt {

	private static final Logger log = LoggerFactory.getLogger(MailDeliveryAttempt.class);

	private final OutboundMailSender mailSender;

	MailDeliveryAttempt(OutboundMailSender mailSender) {
		this.mailSender = mailSender;
	}

	/**
	 * Envia a mensagem e informa se ela foi aceita pelo servidor (ou por que não foi). {@code purpose} e {@code referenceId}
	 * identificam o registro (convite ou recuperação) nos logs sem expor o token.
	 *
	 * <p>Exemplo: {@code boolean sent = deliveryAttempt.tryDeliver(mail, "invitation", invitationId).delivered();}
	 */
	public MailDeliveryResult tryDeliver(OutboundMail mail, String purpose, UUID referenceId) {
		try {
			mailSender.send(mail);
			log.atInfo().addKeyValue("purpose", purpose).addKeyValue("referenceId", referenceId).log("mail delivered");
			return MailDeliveryResult.success();
		}
		catch (MailDeliveryException deliveryFailure) {
			log.atError()
				.addKeyValue("purpose", purpose)
				.addKeyValue("referenceId", referenceId)
				.addKeyValue("failure", deliveryFailure.getMessage())
				.log("mail delivery failed");
			return MailDeliveryResult.failedWith(deliveryFailure.getMessage());
		}
	}

}
