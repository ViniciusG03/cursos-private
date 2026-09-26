package dev.vinicius.cursos.api.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Substitui o I/O externo e o tempo por fakes nomeadas: nenhum teste fala com SMTP ou depende do relógio
 * do sistema. O worker de recuperação continua real e assíncrono (agendador do Spring).
 *
 * <p>Exemplo: {@code @Autowired RecordingMailSender mailbox;}
 */
@TestConfiguration(proxyBeanMethods = false)
public class AccessTestDoublesConfiguration {

	@Bean
	@Primary
	RecordingMailSender recordingMailSender() {
		return new RecordingMailSender();
	}

	@Bean
	@Primary
	MutableTestClock mutableTestClock() {
		return new MutableTestClock();
	}

}
