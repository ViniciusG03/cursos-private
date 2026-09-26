package dev.vinicius.cursos.api.security.attemptlimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vinicius.cursos.api.support.AccessIntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Limites do perfil de teste (15 min): 20 por origem, 5 por e-mail + origem, 8 por e-mail em todas as origens. */
class AttemptLimiterTest extends AccessIntegrationTest {

	@Autowired
	private AttemptLimiter attemptLimiter;

	@Autowired
	private AttemptWindowJanitor windowJanitor;

	@Test
	void allowsAttemptsUpToIdentityLimitThenRejects() {
		recordAttempts(5, "10.0.0.1", "ana@x.com");

		assertThatThrownBy(() -> attemptLimiter.recordAttempt(AttemptScope.LOGIN, "10.0.0.1", "ana@x.com"))
			.isInstanceOf(TooManyAttemptsException.class)
			.hasMessageContaining("at most 5");
	}

	@Test
	void identityLimitIgnoresLetterCase() {
		recordAttempts(5, "10.0.0.1", "ana@x.com");

		assertThatThrownBy(() -> attemptLimiter.recordAttempt(AttemptScope.LOGIN, "10.0.0.1", " ANA@x.com"))
			.isInstanceOf(TooManyAttemptsException.class);
	}

	@Test
	void attackerOnOtherOriginDoesNotLockOutTheOwner() {
		recordAttempts(5, "203.0.113.7", "ana@x.com");

		assertThatCode(() -> attemptLimiter.recordAttempt(AttemptScope.LOGIN, "10.0.0.1", "ana@x.com"))
			.doesNotThrowAnyException();
	}

	@Test
	void distributedAttemptsOnOneIdentityHitTheHigherCeiling() {
		for (int attempt = 0; attempt < 8; attempt++) {
			attemptLimiter.recordAttempt(AttemptScope.LOGIN, "203.0.113." + attempt, "ana@x.com");
		}

		assertThatThrownBy(() -> attemptLimiter.recordAttempt(AttemptScope.LOGIN, "203.0.113.99", "ana@x.com"))
			.isInstanceOf(TooManyAttemptsException.class)
			.hasMessageContaining("at most 8");
	}

	@Test
	void originLimitAppliesAcrossIdentities() {
		for (int attempt = 0; attempt < 20; attempt++) {
			attemptLimiter.recordAttempt(AttemptScope.LOGIN, "10.0.0.1", "user" + attempt + "@x.com");
		}

		assertThatThrownBy(() -> attemptLimiter.recordAttempt(AttemptScope.LOGIN, "10.0.0.1", "nova@x.com"))
			.isInstanceOf(TooManyAttemptsException.class)
			.hasMessageContaining("at most 20");
	}

	@Test
	void scopesAreCountedSeparately() {
		recordAttempts(5, "10.0.0.1", "ana@x.com");

		assertThatCode(() -> attemptLimiter.recordAttempt(AttemptScope.PASSWORD_RESET_REQUEST, "10.0.0.1", "ana@x.com"))
			.doesNotThrowAnyException();
	}

	@Test
	void windowResetsAfterItExpiresAndReportsRetryAfter() {
		recordAttempts(5, "10.0.0.1", "ana@x.com");
		clock.advance(Duration.ofMinutes(10));

		assertThatThrownBy(() -> attemptLimiter.recordAttempt(AttemptScope.LOGIN, "10.0.0.1", "ana@x.com"))
			.isInstanceOfSatisfying(TooManyAttemptsException.class,
					tooMany -> assertThat(tooMany.retryAfterSeconds()).isEqualTo(300));

		clock.advance(Duration.ofMinutes(5));
		assertThatCode(() -> attemptLimiter.recordAttempt(AttemptScope.LOGIN, "10.0.0.1", "ana@x.com"))
			.doesNotThrowAnyException();
	}

	@Test
	void storesOnlyHashedBucketKeys() {
		attemptLimiter.recordAttempt(AttemptScope.LOGIN, "10.0.0.1", "ana@x.com");

		assertThat(jdbcTemplate.queryForList("SELECT bucket_key FROM auth_attempt_windows", String.class)).hasSize(3)
			.allMatch(bucketKey -> bucketKey.matches("[0-9a-f]{64}"));
	}

	@Test
	void janitorPurgesOnlyExpiredWindows() {
		attemptLimiter.recordAttempt(AttemptScope.LOGIN, "10.0.0.1", null);
		clock.advance(Duration.ofMinutes(15));
		attemptLimiter.recordAttempt(AttemptScope.LOGIN, "10.0.0.2", null);

		assertThat(windowJanitor.purgeExpiredWindows()).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM auth_attempt_windows", Integer.class)).isEqualTo(1);
	}

	private void recordAttempts(int attempts, String clientAddress, String identity) {
		for (int attempt = 0; attempt < attempts; attempt++) {
			attemptLimiter.recordAttempt(AttemptScope.LOGIN, clientAddress, identity);
		}
	}

}
