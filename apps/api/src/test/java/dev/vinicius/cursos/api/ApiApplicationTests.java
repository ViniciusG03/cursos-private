package dev.vinicius.cursos.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Import(ApiApplicationTests.PostgresTestConfiguration.class)
class ApiApplicationTests {

	@Test
	void contextLoads() {}

	@TestConfiguration(proxyBeanMethods = false)
	static class PostgresTestConfiguration {

		@Bean
		@ServiceConnection
		PostgreSQLContainer postgres() {
			return new PostgreSQLContainer("postgres:18");
		}

	}

}
