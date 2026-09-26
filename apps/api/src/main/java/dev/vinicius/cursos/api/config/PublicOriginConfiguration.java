package dev.vinicius.cursos.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Valida a origem pública na inicialização. HTTP depende do perfil ativo, não de uma flag: uma variável
 * esquecida no deploy não consegue liberar links HTTP em produção.
 */
@Configuration(proxyBeanMethods = false)
public class PublicOriginConfiguration {

	static final String PLAIN_HTTP_PROFILE = "local";

	/**
	 * Origem usada nos links de e-mail; a aplicação não sobe se ela for inválida.
	 *
	 * <p>Exemplo: {@code publicOrigin(properties, environment).value()}.
	 */
	@Bean
	public PublicOrigin publicOrigin(LibraryAccessProperties accessProperties, Environment environment) {
		return PublicOrigin.parse(accessProperties.publicBaseUrl(), environment.matchesProfiles(PLAIN_HTTP_PROFILE));
	}

}
