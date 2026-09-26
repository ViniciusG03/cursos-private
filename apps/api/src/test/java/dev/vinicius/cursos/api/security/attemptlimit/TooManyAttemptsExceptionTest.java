package dev.vinicius.cursos.api.security.attemptlimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TooManyAttemptsExceptionTest {

	@Test
	void roundsRetryAfterUpToWholeSeconds() {
		assertThat(new TooManyAttemptsException(AttemptScope.LOGIN, 5, Duration.ofMillis(1_500)).retryAfterSeconds())
			.isEqualTo(2);
	}

	@Test
	void neverAsksToRetryInZeroSeconds() {
		assertThat(new TooManyAttemptsException(AttemptScope.LOGIN, 5, Duration.ZERO).retryAfterSeconds()).isEqualTo(1);
	}

}
