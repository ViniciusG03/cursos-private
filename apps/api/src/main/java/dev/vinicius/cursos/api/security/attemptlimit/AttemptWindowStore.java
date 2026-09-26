package dev.vinicius.cursos.api.security.attemptlimit;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Contadores de janela fixa em {@code auth_attempt_windows} (V7). O incremento é um único UPSERT
 * atômico, então requisições concorrentes nunca perdem contagem.
 */
@Repository
class AttemptWindowStore {

	private static final String INCREMENT_SQL = """
			INSERT INTO auth_attempt_windows AS w (bucket_key, window_started_at, attempt_count)
			VALUES (?, ?, 1)
			ON CONFLICT (bucket_key) DO UPDATE SET
			    attempt_count = CASE WHEN w.window_started_at <= ? THEN 1 ELSE w.attempt_count + 1 END,
			    window_started_at = CASE WHEN w.window_started_at <= ? THEN EXCLUDED.window_started_at
			                             ELSE w.window_started_at END
			RETURNING attempt_count, window_started_at""";

	private final JdbcTemplate jdbcTemplate;

	AttemptWindowStore(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Conta mais uma tentativa no balde. Uma janela iniciada em {@code expiredUpTo} ou antes recomeça em 1.
	 *
	 * <p>Exemplo: {@code AttemptWindow window = store.increment(bucketKey, now, now.minus(window));}
	 */
	AttemptWindow increment(String bucketKey, Instant now, Instant expiredUpTo) {
		Timestamp expiredThreshold = Timestamp.from(expiredUpTo);
		return jdbcTemplate.queryForObject(INCREMENT_SQL,
				(row, rowNumber) -> new AttemptWindow(row.getInt("attempt_count"),
						row.getTimestamp("window_started_at").toInstant()),
				bucketKey, Timestamp.from(now), expiredThreshold, expiredThreshold);
	}

	/**
	 * Apaga janelas vencidas; é o que mantém a tabela limitada ao tráfego de uma janela.
	 *
	 * <p>Exemplo: {@code int purged = store.deleteExpired(now.minus(window));}
	 */
	int deleteExpired(Instant expiredUpTo) {
		return jdbcTemplate.update("DELETE FROM auth_attempt_windows WHERE window_started_at <= ?",
				Timestamp.from(expiredUpTo));
	}

	/** Estado do balde após o incremento. */
	record AttemptWindow(int attemptCount, Instant windowStartedAt) {}

}
