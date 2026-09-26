package dev.vinicius.cursos.api.passwordreset.domain;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Pedido público de recuperação aguardando processamento, mapeado sobre {@code password_reset_requests}
 * (V8). Não guarda token: ele é emitido só no momento do envio.
 */
@Entity
@Table(name = "password_reset_requests")
public class PasswordResetRequest {

	static final int LAST_ERROR_MAX_LENGTH = 1000;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "email", nullable = false, length = AccountEmail.MAX_LENGTH)
	private String email;

	@Column(name = "requested_at", nullable = false)
	private Instant requestedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PasswordResetRequestStatus status;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "next_attempt_at", nullable = false)
	private Instant nextAttemptAt;

	@Column(name = "last_attempt_at")
	private Instant lastAttemptAt;

	@Column(name = "last_error", length = LAST_ERROR_MAX_LENGTH)
	private String lastError;

	@Column(name = "completed_at")
	private Instant completedAt;

	protected PasswordResetRequest() {}

	private PasswordResetRequest(AccountEmail email, Instant now) {
		this.email = email.value();
		this.requestedAt = now;
		this.status = PasswordResetRequestStatus.PENDING;
		this.nextAttemptAt = now;
	}

	/**
	 * Enfileira um pedido pronto para o worker, exista ou não conta com o e-mail.
	 *
	 * <p>Exemplo: {@code requestRepository.save(PasswordResetRequest.queue(email, clock.instant()))}.
	 */
	public static PasswordResetRequest queue(AccountEmail email, Instant now) {
		return new PasswordResetRequest(email, now);
	}

	/**
	 * Conclui o pedido com o link enviado.
	 *
	 * <p>Exemplo: {@code request.markSent(now);}
	 */
	public void markSent(Instant now) {
		complete(PasswordResetRequestStatus.SENT, now);
	}

	/**
	 * Conclui o pedido de um e-mail sem conta, sem enviar nada.
	 *
	 * <p>Exemplo: {@code request.markNoAccount(now);}
	 */
	public void markNoAccount(Instant now) {
		complete(PasswordResetRequestStatus.NO_ACCOUNT, now);
	}

	/**
	 * Registra uma falha e agenda a próxima tentativa com espera dobrada ({@code backoff}, 2x, 4x...). Ao
	 * atingir {@code maxAttempts}, conclui como {@code FAILED}.
	 *
	 * <p>Exemplo: {@code request.recordFailedAttempt(now, "SMTP ... failed", 5, Duration.ofMinutes(1));}
	 */
	public void recordFailedAttempt(Instant now, String failure, int maxAttempts, Duration backoff) {
		attemptCount++;
		lastAttemptAt = now;
		lastError = truncate(failure);
		if (attemptCount >= maxAttempts) {
			complete(PasswordResetRequestStatus.FAILED, now);
			return;
		}
		nextAttemptAt = now.plus(backoff.multipliedBy(1L << (attemptCount - 1)));
	}

	private void complete(PasswordResetRequestStatus finalStatus, Instant now) {
		this.status = finalStatus;
		this.completedAt = now;
		this.lastAttemptAt = now;
	}

	private static String truncate(String failure) {
		if (failure == null || failure.length() <= LAST_ERROR_MAX_LENGTH) {
			return failure;
		}
		return failure.substring(0, LAST_ERROR_MAX_LENGTH);
	}

	public UUID getId() {
		return id;
	}

	public AccountEmail getEmail() {
		return new AccountEmail(email);
	}

	public PasswordResetRequestStatus getStatus() {
		return status;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public Instant getNextAttemptAt() {
		return nextAttemptAt;
	}

	public String getLastError() {
		return lastError;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

}
