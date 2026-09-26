package dev.vinicius.cursos.api.support;

import dev.vinicius.cursos.api.mail.MailDeliveryException;
import dev.vinicius.cursos.api.mail.OutboundMail;
import dev.vinicius.cursos.api.mail.OutboundMailSender;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fake de {@link OutboundMailSender}: guarda as mensagens em memória e pode simular falha de SMTP.
 *
 * <p>Exemplo: {@code String token = mailbox.lastTokenSentTo("ana@x.com");}
 */
public class RecordingMailSender implements OutboundMailSender {

	private static final Pattern TOKEN_IN_LINK = Pattern.compile("#token=([A-Za-z0-9_-]+)");

	private final List<OutboundMail> deliveredMails = new CopyOnWriteArrayList<>();

	private final List<String> deliveringThreads = new CopyOnWriteArrayList<>();

	private volatile boolean failing;

	private volatile boolean crashing;

	private volatile CountDownLatch deliveryEntered = new CountDownLatch(1);

	private volatile CountDownLatch deliveryGate;

	@Override
	public void send(OutboundMail mail) {
		passThroughGate();
		if (crashing) {
			throw new IllegalStateException("simulated unexpected failure inside the mail adapter");
		}
		if (failing) {
			throw new MailDeliveryException("SMTP delivery to %s failed: simulated outage".formatted(mail.recipient()),
					null);
		}
		deliveredMails.add(mail);
		deliveringThreads.add(Thread.currentThread().getName());
	}

	/** Liga ou desliga a falha simulada de SMTP. */
	public void simulateOutage(boolean failing) {
		this.failing = failing;
	}

	/**
	 * Faz os próximos envios pararem dentro de {@link #send} (com as locks do worker ainda seguras) até
	 * {@link #releaseHeldDeliveries()}.
	 */
	public void holdDeliveries() {
		deliveryEntered = new CountDownLatch(1);
		deliveryGate = new CountDownLatch(1);
	}

	/** Espera (até 10 s) algum worker chegar ao envio segurado por {@link #holdDeliveries()}. */
	public void awaitDeliveryHeld() {
		awaitLatch(deliveryEntered, "a worker to reach the held delivery");
	}

	/** Libera os envios segurados; eles seguem com a falha simulada, se houver. */
	public void releaseHeldDeliveries() {
		CountDownLatch gate = deliveryGate;
		deliveryGate = null;
		if (gate != null) {
			gate.countDown();
		}
	}

	/** Faz o envio lançar uma exceção inesperada (não {@code MailDeliveryException}), como um bug no adaptador. */
	public void simulateCrash(boolean crashing) {
		this.crashing = crashing;
	}

	/** Esvazia a caixa e volta a entregar normalmente. */
	public void reset() {
		releaseHeldDeliveries();
		crashing = false;
		deliveredMails.clear();
		deliveringThreads.clear();
		failing = false;
	}

	public List<OutboundMail> deliveredMails() {
		return List.copyOf(deliveredMails);
	}

	/** Nomes das threads que entregaram cada mensagem, na ordem de entrega. */
	public List<String> deliveringThreads() {
		return List.copyOf(deliveringThreads);
	}

	/**
	 * Espera (até 10 s) o {@code mailNumber}-ésimo e-mail ao destinatário, entregue por um worker em outra
	 * thread, e devolve o token do link dele.
	 *
	 * <p>Exemplo: {@code String secondToken = mailbox.awaitTokenSentTo("ana@x.com", 2);}
	 */
	public String awaitTokenSentTo(String normalizedEmail, int mailNumber) {
		Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
		while (countSentTo(normalizedEmail) < mailNumber) {
			if (Instant.now().isAfter(deadline)) {
				throw new AssertionError("expected %d mails to %s within 10s, got %d".formatted(mailNumber,
						normalizedEmail, countSentTo(normalizedEmail)));
			}
			sleepBriefly();
		}
		return tokenOf(deliveredMails.stream()
			.filter(mail -> mail.recipient().value().equals(normalizedEmail))
			.toList()
			.get(mailNumber - 1));
	}

	private void passThroughGate() {
		CountDownLatch gate = deliveryGate;
		if (gate == null) {
			return;
		}
		deliveryEntered.countDown();
		awaitLatch(gate, "the test to release the held delivery");
	}

	private static void awaitLatch(CountDownLatch latch, String description) {
		try {
			if (!latch.await(10, TimeUnit.SECONDS)) {
				throw new AssertionError("expected " + description + " within 10s, got timeout");
			}
		}
		catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new AssertionError("interrupted while waiting for " + description, interrupted);
		}
	}

	private long countSentTo(String normalizedEmail) {
		return deliveredMails.stream().filter(mail -> mail.recipient().value().equals(normalizedEmail)).count();
	}

	private static void sleepBriefly() {
		try {
			Thread.sleep(20);
		}
		catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new AssertionError("interrupted while waiting for mail", interrupted);
		}
	}

	/**
	 * Token do link do último e-mail entregue ao destinatário.
	 *
	 * <p>Exemplo: {@code mailbox.lastTokenSentTo("ana@x.com")}.
	 */
	public String lastTokenSentTo(String normalizedEmail) {
		OutboundMail lastMail = deliveredMails.reversed()
			.stream()
			.filter(mail -> mail.recipient().value().equals(normalizedEmail))
			.findFirst()
			.orElseThrow(() -> new AssertionError("expected a mail to %s, got recipients %s".formatted(normalizedEmail,
					deliveredMails.stream().map(OutboundMail::recipient).toList())));
		return tokenOf(lastMail);
	}

	private static String tokenOf(OutboundMail mail) {
		Matcher tokenMatcher = TOKEN_IN_LINK.matcher(mail.body());
		if (!tokenMatcher.find()) {
			throw new AssertionError("expected a '#token=' link in the mail body, got none");
		}
		return tokenMatcher.group(1);
	}

}
