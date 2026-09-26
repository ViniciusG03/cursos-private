package dev.vinicius.cursos.api;

import dev.vinicius.cursos.api.support.PostgresTestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestcontainersConfiguration.class)
class ApiApplicationTests {

	@Test
	void contextLoads() {}

}
