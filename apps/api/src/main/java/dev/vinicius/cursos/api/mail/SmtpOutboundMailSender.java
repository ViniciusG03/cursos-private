package dev.vinicius.cursos.api.mail;

import dev.vinicius.cursos.api.config.LibraryAccessProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Adaptador de {@link OutboundMailSender} sobre o {@link JavaMailSender} do Spring Boot. Host, porta,
 * credenciais, TLS e timeouts vêm de {@code spring.mail.*} por ambiente; nada disso fica no Git.
 */
@Component
class SmtpOutboundMailSender implements OutboundMailSender {

	private final JavaMailSender javaMailSender;

	private final String senderAddress;

	SmtpOutboundMailSender(JavaMailSender javaMailSender, LibraryAccessProperties accessProperties) {
		this.javaMailSender = javaMailSender;
		this.senderAddress = accessProperties.mailFrom();
	}

	@Override
	public void send(OutboundMail mail) {
		try {
			javaMailSender.send(toSimpleMessage(mail));
		}
		catch (MailException smtpFailure) {
			throw new MailDeliveryException("SMTP delivery to %s failed: %s: %s".formatted(mail.recipient(),
					smtpFailure.getClass().getSimpleName(), smtpFailure.getMessage()), smtpFailure);
		}
	}

	private SimpleMailMessage toSimpleMessage(OutboundMail mail) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(senderAddress);
		message.setTo(mail.recipient().value());
		message.setSubject(mail.subject());
		message.setText(mail.body());
		return message;
	}

}
