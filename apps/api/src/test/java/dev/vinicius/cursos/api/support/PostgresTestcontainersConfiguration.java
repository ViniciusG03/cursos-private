package dev.vinicius.cursos.api.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL real para os testes. Todas as classes {@code @SpringBootTest} importam esta mesma
 * configuração para que o Spring reaproveite o contexto e, com ele, um único container.
 *
 * <p>Exemplo: {@code @Import(PostgresTestcontainersConfiguration.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgres() {
		return new PostgreSQLContainer("postgres:18");
	}

}
