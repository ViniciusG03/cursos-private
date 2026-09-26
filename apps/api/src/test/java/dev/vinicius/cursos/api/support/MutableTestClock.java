package dev.vinicius.cursos.api.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Relógio de teste parado num instante com segundos inteiros (sem frações, então o que vai ao
 * timestamptz volta idêntico) e que só avança quando o teste manda.
 *
 * <p>Exemplo: {@code clock.advance(Duration.ofHours(72));}
 */
public class MutableTestClock extends Clock {

	public static final Instant START = Instant.parse("2026-01-05T12:00:00Z");

	private volatile Instant currentInstant = START;

	/** Volta ao instante inicial. */
	public void reset() {
		currentInstant = START;
	}

	/** Avança o relógio pela duração informada. */
	public void advance(Duration duration) {
		currentInstant = currentInstant.plus(duration);
	}

	@Override
	public Instant instant() {
		return currentInstant;
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		throw new UnsupportedOperationException("MutableTestClock is UTC-only, got request for zone " + zone);
	}

}
