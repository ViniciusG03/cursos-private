package dev.vinicius.cursos.api.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.config.LibraryAccessProperties;
import dev.vinicius.cursos.api.config.LibraryAccessProperties.AttemptLimit;
import dev.vinicius.cursos.api.config.LibraryAccessProperties.PasswordResetDispatch;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class SmtpOutboundMailSenderTest {

	private static final OutboundMail MAIL = new OutboundMail(AccountEmail.parse("ana@x.com"), "Convite",
			"Acesse https://x/convites/aceitar#token=segredo123");

	@Test
	void sendsPlainTextFromConfiguredSender() {
		CapturingJavaMailSender smtp = new CapturingJavaMailSender(false);

		senderOver(smtp).send(MAIL);

		SimpleMailMessage sent = smtp.sentMessages.getFirst();
		assertThat(sent.getFrom()).isEqualTo("biblioteca@x.com");
		assertThat(sent.getTo()).containsExactly("ana@x.com");
		assertThat(sent.getSubject()).isEqualTo("Convite");
		assertThat(sent.getText()).isEqualTo(MAIL.body());
	}

	@Test
	void smtpFailureBecomesDeliveryExceptionWithoutBody() {
		SmtpOutboundMailSender sender = senderOver(new CapturingJavaMailSender(true));

		assertThatThrownBy(() -> sender.send(MAIL)).isInstanceOf(MailDeliveryException.class)
			.hasMessageContaining("ana@x.com")
			.hasMessageContaining("connection refused")
			.hasMessageNotContaining("segredo123");
	}

	private static SmtpOutboundMailSender senderOver(JavaMailSenderImpl smtp) {
		AttemptLimit attemptLimit = new AttemptLimit(Duration.ofMinutes(15), 30, 10, 100, Duration.ofMinutes(10));
		PasswordResetDispatch dispatch = new PasswordResetDispatch(Duration.ofSeconds(5), 5, Duration.ofMinutes(1),
				Duration.ofDays(30));
		return new SmtpOutboundMailSender(smtp,
				new LibraryAccessProperties(URI.create("https://x"), "biblioteca@x.com", attemptLimit, dispatch));
	}

	/** Fake do SMTP: guarda as mensagens ou simula recusa de conexão, sem abrir socket. */
	private static final class CapturingJavaMailSender extends JavaMailSenderImpl {

		private final boolean refusing;

		private final List<SimpleMailMessage> sentMessages = new ArrayList<>();

		private CapturingJavaMailSender(boolean refusing) {
			this.refusing = refusing;
		}

		@Override
		public void send(SimpleMailMessage simpleMessage) {
			if (refusing) {
				throw new MailSendException("connection refused");
			}
			sentMessages.add(simpleMessage);
		}

	}

}
