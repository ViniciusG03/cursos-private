package dev.vinicius.cursos.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PublicOriginTest {

	@Test
	void acceptsHttpsOriginAndDropsTrailingSlash() {
		assertThat(PublicOrigin.parse(URI.create("https://Cursos.Example.com/"), false).value())
			.isEqualTo("https://cursos.example.com");
	}

	@Test
	void keepsExplicitPort() {
		assertThat(PublicOrigin.parse(URI.create("https://cursos.example.com:8443"), false).value())
			.isEqualTo("https://cursos.example.com:8443");
	}

	@Test
	void rejectsPlainHttpOutsideLocalProfile() {
		assertThatThrownBy(() -> PublicOrigin.parse(URI.create("http://cursos.example.com"), false))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("LIBRARY_PUBLIC_BASE_URL")
			.hasMessageContaining("https URL (http is accepted only in the local profile)")
			.hasMessageContaining("'http://cursos.example.com'");
	}

	@Test
	void acceptsPlainHttpWhenLocalProfileAllowsIt() {
		assertThat(PublicOrigin.parse(URI.create("http://localhost:5173"), true).value())
			.isEqualTo("http://localhost:5173");
	}

	@ParameterizedTest
	@ValueSource(strings = { "ftp://cursos.example.com", "cursos.example.com", "https:///sem-host",
			"https://cursos.example.com/app", "https://cursos.example.com?x=1", "https://cursos.example.com#frag",
			"https://user:senha@cursos.example.com" })
	void rejectsAnythingThatIsNotAnOrigin(String configuredUrl) {
		assertThatThrownBy(() -> PublicOrigin.parse(URI.create(configuredUrl), true))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("LIBRARY_PUBLIC_BASE_URL");
	}

	@Test
	void rejectsMissingUrl() {
		assertThatThrownBy(() -> PublicOrigin.parse(null, true)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void buildsLinkWithTokenInFragment() {
		PublicOrigin origin = PublicOrigin.parse(URI.create("https://cursos.example.com"), false);

		assertThat(origin.linkWithToken("/convites/aceitar", "abc")).isEqualTo("https://cursos.example.com/convites/aceitar#token=abc");
	}

}
