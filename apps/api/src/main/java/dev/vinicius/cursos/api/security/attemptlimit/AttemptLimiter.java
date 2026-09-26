package dev.vinicius.cursos.api.security.attemptlimit;

import dev.vinicius.cursos.api.config.LibraryAccessProperties;
import dev.vinicius.cursos.api.config.LibraryAccessProperties.AttemptLimit;
import dev.vinicius.cursos.api.security.attemptlimit.AttemptWindowStore.AttemptWindow;
import dev.vinicius.cursos.api.support.Sha256Hex;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Limita tentativas nos fluxos públicos por origem (IP) e, quando disponível, por identidade (e-mail).
 * Toda tentativa conta, com ou sem sucesso: assim o limite não revela se a credencial estava certa.
 *
 * <p>O limite estrito de identidade vale por par (e-mail + origem): um atacante em outro IP não
 * consegue bloquear o login nem a recuperação do dono da conta. O teto por e-mail somando todas as
 * origens é bem mais alto e só segura ataques distribuídos.
 */
@Component
public class AttemptLimiter {

	private static final Logger log = LoggerFactory.getLogger(AttemptLimiter.class);

	private final AttemptWindowStore windowStore;

	private final AttemptLimit attemptLimit;

	private final Clock clock;

	AttemptLimiter(AttemptWindowStore windowStore, LibraryAccessProperties accessProperties, Clock clock) {
		this.windowStore = windowStore;
		this.attemptLimit = accessProperties.attemptLimit();
		this.clock = clock;
	}

	/**
	 * Registra uma tentativa e lança {@link TooManyAttemptsException} se a origem, o par identidade +
	 * origem ou a identidade em todas as origens passou do limite na janela atual. {@code identity} pode
	 * ser nulo (ex.: consumo de token).
	 *
	 * <p>Exemplo: {@code attemptLimiter.recordAttempt(AttemptScope.LOGIN, request.getRemoteAddr(), email);}
	 */
	public void recordAttempt(AttemptScope scope, String clientAddress, String identity) {
		Instant now = clock.instant();
		requireWithinLimit(scope, "client", clientAddress, attemptLimit.maxAttemptsPerClient(), now);
		if (identity != null && !identity.isBlank()) {
			requireWithinIdentityLimits(scope, clientAddress, identity.strip().toLowerCase(Locale.ROOT), now);
		}
	}

	private void requireWithinIdentityLimits(AttemptScope scope, String clientAddress, String identity, Instant now) {
		requireWithinLimit(scope, "identity-client", identity + "|" + clientAddress,
				attemptLimit.maxAttemptsPerIdentity(), now);
		requireWithinLimit(scope, "identity", identity, attemptLimit.maxAttemptsPerIdentityAllClients(), now);
	}

	private void requireWithinLimit(AttemptScope scope, String keyKind, String keyValue, int maxAttempts,
			Instant now) {
		String bucketKey = Sha256Hex.of(scope + "|" + keyKind + "|" + keyValue);
		AttemptWindow window = windowStore.increment(bucketKey, now, now.minus(attemptLimit.window()));
		if (window.attemptCount() <= maxAttempts) {
			return;
		}
		Duration retryAfter = Duration.between(now, window.windowStartedAt().plus(attemptLimit.window()));
		log.atWarn()
			.addKeyValue("scope", scope)
			.addKeyValue("keyKind", keyKind)
			.addKeyValue("attemptCount", window.attemptCount())
			.log("attempt limit exceeded");
		throw new TooManyAttemptsException(scope, maxAttempts, retryAfter);
	}

}
