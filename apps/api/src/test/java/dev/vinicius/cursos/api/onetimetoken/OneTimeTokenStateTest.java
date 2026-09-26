package dev.vinicius.cursos.api.onetimetoken;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class OneTimeTokenStateTest {

	private static final Instant EXPIRES_AT = Instant.parse("2026-01-05T12:30:00Z");

	@Test
	void usableOneMicrosecondBeforeExpiry() {
		assertThat(OneTimeTokenState.isUsable(null, null, EXPIRES_AT, EXPIRES_AT.minusNanos(1_000))).isTrue();
	}

	@Test
	void expiredExactlyAtExpiryInstant() {
		assertThat(OneTimeTokenState.isUsable(null, null, EXPIRES_AT, EXPIRES_AT)).isFalse();
	}

	@Test
	void consumedOrRevokedIsNeverUsable() {
		Instant earlier = EXPIRES_AT.minusSeconds(60);

		assertThat(OneTimeTokenState.isUsable(earlier, null, EXPIRES_AT, earlier)).isFalse();
		assertThat(OneTimeTokenState.isUsable(null, earlier, EXPIRES_AT, earlier)).isFalse();
	}

}
