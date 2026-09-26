package dev.vinicius.cursos.api.passwordreset.service;

import dev.vinicius.cursos.api.config.LibraryAccessProperties;
import dev.vinicius.cursos.api.config.LibraryAccessProperties.PasswordResetDispatch;
import dev.vinicius.cursos.api.mail.AccessMailComposer;
import dev.vinicius.cursos.api.mail.MailDeliveryAttempt;
import dev.vinicius.cursos.api.mail.MailDeliveryResult;
import dev.vinicius.cursos.api.mail.OutboundMail;
import dev.vinicius.cursos.api.passwordreset.domain.PasswordResetRequest;
import dev.vinicius.cursos.api.passwordreset.domain.PasswordResetRequestStatus;
import dev.vinicius.cursos.api.passwordreset.repository.PasswordResetRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Worker da fila {@code password_reset_requests}. Cada tentativa roda numa única transação que trava a
 * linha, emite o token, envia o e-mail e grava o resultado ({@code SENT}, {@code NO_ACCOUNT} ou a falha
 * com nova tentativa agendada) antes de soltar o lock. Assim nenhum outro worker enxerga o pedido entre o
 * envio e o registro do resultado. Se o e-mail falhar, o token emitido é revogado na mesma transação.
 *
 * <p>Ordem de locks: pedido → conta → tokens, a mesma de {@link PasswordResetConfirmation}.
 */
@Component
public class PasswordResetDispatcher {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetDispatcher.class);

	static final int BATCH_SIZE = 50;

	private final PasswordResetRequestRepository requestRepository;

	private final PasswordResetRegistry resetRegistry;

	private final AccessMailComposer mailComposer;

	private final MailDeliveryAttempt deliveryAttempt;

	private final TransactionTemplate transactionTemplate;

	private final PasswordResetDispatch dispatchSettings;

	private final Clock clock;

	PasswordResetDispatcher(PasswordResetRequestRepository requestRepository, PasswordResetRegistry resetRegistry,
			AccessMailComposer mailComposer, MailDeliveryAttempt deliveryAttempt,
			TransactionTemplate transactionTemplate, LibraryAccessProperties accessProperties, Clock clock) {
		this.requestRepository = requestRepository;
		this.resetRegistry = resetRegistry;
		this.mailComposer = mailComposer;
		this.deliveryAttempt = deliveryAttempt;
		this.transactionTemplate = transactionTemplate;
		this.dispatchSettings = accessProperties.passwordResetDispatch();
		this.clock = clock;
	}

	/**
	 * Processa até {@link #BATCH_SIZE} pedidos vencidos e devolve quantos tratou. Seguro com várias threads
	 * ou instâncias ao mesmo tempo ({@code FOR UPDATE SKIP LOCKED}).
	 *
	 * <p>Exemplo: {@code int handled = dispatcher.dispatchDue();}
	 */
	public int dispatchDue() {
		int handledRequests = 0;
		while (handledRequests < BATCH_SIZE && dispatchNext()) {
			handledRequests++;
		}
		return handledRequests;
	}

	/**
	 * Apaga pedidos concluídos há mais que a retenção configurada.
	 *
	 * <p>Exemplo: {@code int purged = dispatcher.purgeCompleted();}
	 */
	public int purgeCompleted() {
		Instant threshold = clock.instant().minus(dispatchSettings.retention());
		Integer purgedRequests = transactionTemplate.execute(tx -> requestRepository.deleteCompletedBefore(threshold));
		return purgedRequests == null ? 0 : purgedRequests;
	}

	// Uma exceção inesperada (banco, bug) desfaz a transação inteira, inclusive o token; o pedido volta a
	// ficar visível e a falha é registrada numa transação nova, mas só se ninguém o tratou nesse meio-tempo.
	private boolean dispatchNext() {
		AtomicReference<ClaimedRequest> claimed = new AtomicReference<>();
		try {
			return Boolean.TRUE.equals(transactionTemplate.execute(tx -> attemptNextDue(claimed)));
		}
		catch (RuntimeException unexpectedFailure) {
			ClaimedRequest claimedRequest = claimed.get();
			if (claimedRequest == null) {
				throw unexpectedFailure;
			}
			transactionTemplate.executeWithoutResult(
					tx -> recordUnexpectedFailure(claimedRequest, unexpectedFailure.getClass().getSimpleName()));
			return true;
		}
	}

	private boolean attemptNextDue(AtomicReference<ClaimedRequest> claimed) {
		Instant now = clock.instant();
		Optional<PasswordResetRequest> next = requestRepository.lockNextDue(now);
		if (next.isEmpty()) {
			return false;
		}
		PasswordResetRequest request = next.get();
		claimed.set(new ClaimedRequest(request.getId(), request.getAttemptCount()));
		attemptDelivery(request, now);
		return true;
	}

	private void attemptDelivery(PasswordResetRequest request, Instant now) {
		Optional<IssuedPasswordReset> issued = resetRegistry.issueFor(request.getEmail());
		if (issued.isEmpty()) {
			request.markNoAccount(now);
			logCompleted(request);
			return;
		}
		MailDeliveryResult delivery = deliveryAttempt.tryDeliver(composeMail(issued.get()), "password-reset",
				request.getId());
		if (delivery.delivered()) {
			request.markSent(now);
			logCompleted(request);
			return;
		}
		// Um link que não chegou a ninguém não pode continuar válido.
		resetRegistry.revokeUndelivered(issued.get(), now);
		recordFailedAttempt(request, now, delivery.failure());
	}

	/**
	 * Registra uma falha inesperada como tentativa, só se o pedido continua exatamente como estava quando
	 * este worker o pegou (ainda {@code PENDING} e com o mesmo número de tentativas). Se outro worker já o
	 * enviou ou registrou outra tentativa, a falha antiga é descartada em vez de sobrescrever o resultado.
	 *
	 * <p>Exemplo: {@code recordUnexpectedFailure(new ClaimedRequest(id, 0), "DataAccessException");}
	 */
	void recordUnexpectedFailure(ClaimedRequest claimedRequest, String failure) {
		Optional<PasswordResetRequest> unchanged = requestRepository.lockById(claimedRequest.requestId())
			.filter(request -> request.getStatus() == PasswordResetRequestStatus.PENDING)
			.filter(request -> request.getAttemptCount() == claimedRequest.observedAttemptCount());
		if (unchanged.isEmpty()) {
			log.atWarn()
				.addKeyValue("requestId", claimedRequest.requestId())
				.addKeyValue("failure", failure)
				.log("stale password reset failure ignored; another worker already handled the request");
			return;
		}
		recordFailedAttempt(unchanged.get(), clock.instant(), failure);
	}

	private void recordFailedAttempt(PasswordResetRequest request, Instant now, String failure) {
		request.recordFailedAttempt(now, failure, dispatchSettings.maxAttempts(), dispatchSettings.retryBackoff());
		log.atError()
			.addKeyValue("requestId", request.getId())
			.addKeyValue("attemptCount", request.getAttemptCount())
			.addKeyValue("status", request.getStatus())
			.addKeyValue("nextAttemptAt", request.getNextAttemptAt())
			.addKeyValue("failure", request.getLastError())
			.log("password reset delivery attempt failed");
	}

	private OutboundMail composeMail(IssuedPasswordReset issued) {
		return mailComposer.passwordReset(issued.email(), issued.rawToken(), issued.expiresAt());
	}

	private static void logCompleted(PasswordResetRequest request) {
		log.atInfo()
			.addKeyValue("requestId", request.getId())
			.addKeyValue("status", request.getStatus())
			.log("password reset request completed");
	}

	/** Pedido pego por este worker e o número de tentativas que ele tinha nesse momento. */
	record ClaimedRequest(UUID requestId, int observedAttemptCount) {}

}
