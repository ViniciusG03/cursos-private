package dev.vinicius.cursos.api.security.attemptlimit;

import dev.vinicius.cursos.api.config.LibraryAccessProperties;
import dev.vinicius.cursos.api.config.LibraryAccessProperties.AttemptLimit;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Remove periodicamente as janelas vencidas de {@code auth_attempt_windows}, no intervalo
 * {@code library.access.attempt-limit.cleanup-interval}. Sem isso a tabela cresceria com cada IP visto.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class AttemptWindowJanitor implements SchedulingConfigurer {

	private static final Logger log = LoggerFactory.getLogger(AttemptWindowJanitor.class);

	private final AttemptWindowStore windowStore;

	private final AttemptLimit attemptLimit;

	private final Clock clock;

	AttemptWindowJanitor(AttemptWindowStore windowStore, LibraryAccessProperties accessProperties, Clock clock) {
		this.windowStore = windowStore;
		this.attemptLimit = accessProperties.attemptLimit();
		this.clock = clock;
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addFixedDelayTask(this::purgeExpiredWindows, attemptLimit.cleanupInterval());
	}

	/**
	 * Apaga as janelas que já não limitam ninguém.
	 *
	 * <p>Exemplo: {@code int purged = janitor.purgeExpiredWindows();}
	 */
	public int purgeExpiredWindows() {
		int purgedWindows = windowStore.deleteExpired(clock.instant().minus(attemptLimit.window()));
		log.atDebug().addKeyValue("purgedWindows", purgedWindows).log("expired attempt windows purged");
		return purgedWindows;
	}

}
