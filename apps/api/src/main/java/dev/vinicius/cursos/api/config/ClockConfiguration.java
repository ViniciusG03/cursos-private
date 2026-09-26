package dev.vinicius.cursos.api.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio único da aplicação, injetado nos serviços para que expiração seja testável. */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

	// UTC e com precisão de microssegundos, a mesma do timestamptz: o instante gravado é o instante comparado.
	@Bean
	Clock clock() {
		return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
	}

}
