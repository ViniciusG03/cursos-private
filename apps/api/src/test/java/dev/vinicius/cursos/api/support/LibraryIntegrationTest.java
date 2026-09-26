package dev.vinicius.cursos.api.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Configuração única dos testes de integração: PostgreSQL via Testcontainers, fakes de I/O externo,
 * perfil {@code test} e MockMvc com a cadeia real do Spring Security. Toda classe {@code @SpringBootTest}
 * usa esta mesma anotação para que o Spring reaproveite um só contexto (e um só container).
 *
 * <p>Exemplo: {@code @LibraryIntegrationTest class MeuTeste { ... }}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ PostgresTestcontainersConfiguration.class, AccessTestDoublesConfiguration.class })
public @interface LibraryIntegrationTest {}
