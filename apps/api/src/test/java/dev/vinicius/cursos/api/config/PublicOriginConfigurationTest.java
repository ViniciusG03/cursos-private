package dev.vinicius.cursos.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vinicius.cursos.api.config.LibraryAccessProperties.AttemptLimit;
import dev.vinicius.cursos.api.config.LibraryAccessProperties.PasswordResetDispatch;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** O perfil ativo decide se HTTP é aceito; nenhuma propriedade consegue liberar HTTP em produção. */
class PublicOriginConfigurationTest {

	private final PublicOriginConfiguration configuration = new PublicOriginConfiguration();

	@Test
	void localProfileAcceptsHttp() {
		MockEnvironment localEnvironment = new MockEnvironment();
		localEnvironment.setActiveProfiles("local");

		assertThat(configuration.publicOrigin(propertiesWith("http://localhost:5173"), localEnvironment).value())
			.isEqualTo("http://localhost:5173");
	}

	@Test
	void anyOtherProfileRequiresHttps() {
		MockEnvironment productionEnvironment = new MockEnvironment();
		productionEnvironment.setActiveProfiles("prod");

		assertThatThrownBy(() -> configuration.publicOrigin(propertiesWith("http://cursos.example.com"),
				productionEnvironment))
			.isInstanceOf(IllegalArgumentException.class);
		assertThat(configuration.publicOrigin(propertiesWith("https://cursos.example.com"), productionEnvironment)
			.value()).isEqualTo("https://cursos.example.com");
	}

	@Test
	void noActiveProfileRequiresHttps() {
		assertThatThrownBy(() -> configuration.publicOrigin(propertiesWith("http://cursos.example.com"),
				new MockEnvironment()))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private static LibraryAccessProperties propertiesWith(String publicBaseUrl) {
		return new LibraryAccessProperties(URI.create(publicBaseUrl), "biblioteca@x.com",
				new AttemptLimit(Duration.ofMinutes(15), 30, 10, 100, Duration.ofMinutes(10)),
				new PasswordResetDispatch(Duration.ofSeconds(5), 5, Duration.ofMinutes(1), Duration.ofDays(30)));
	}

}
