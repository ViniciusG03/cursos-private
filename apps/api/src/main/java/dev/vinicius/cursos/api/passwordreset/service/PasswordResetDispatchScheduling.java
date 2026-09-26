package dev.vinicius.cursos.api.passwordreset.service;

import dev.vinicius.cursos.api.config.LibraryAccessProperties;
import java.time.Duration;
import java.util.function.IntSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Agenda o {@link PasswordResetDispatcher}: varredura a cada
 * {@code library.access.password-reset-dispatch.poll-interval} e limpeza dos concluídos a cada hora. Uma
 * falha inesperada numa rodada é registrada e a próxima rodada tenta de novo.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class PasswordResetDispatchScheduling implements SchedulingConfigurer {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetDispatchScheduling.class);

	static final Duration PURGE_INTERVAL = Duration.ofHours(1);

	private final PasswordResetDispatcher dispatcher;

	private final Duration pollInterval;

	PasswordResetDispatchScheduling(PasswordResetDispatcher dispatcher, LibraryAccessProperties accessProperties) {
		this.dispatcher = dispatcher;
		this.pollInterval = accessProperties.passwordResetDispatch().pollInterval();
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addFixedDelayTask(() -> runLogged("dispatch", dispatcher::dispatchDue), pollInterval);
		taskRegistrar.addFixedDelayTask(() -> runLogged("purge", dispatcher::purgeCompleted), PURGE_INTERVAL);
	}

	private static void runLogged(String taskName, IntSupplier task) {
		try {
			task.getAsInt();
		}
		catch (RuntimeException roundFailure) {
			log.atError()
				.addKeyValue("task", taskName)
				.addKeyValue("failure", roundFailure.getClass().getSimpleName())
				.log("password reset scheduled task failed; retrying next round");
		}
	}

}
