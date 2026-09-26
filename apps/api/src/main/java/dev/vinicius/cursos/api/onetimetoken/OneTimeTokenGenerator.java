package dev.vinicius.cursos.api.onetimetoken;

/**
 * Fonte de tokens brutos para convites e recuperação de acesso. É uma interface do projeto para que
 * os testes injetem uma implementação determinística sem trocar o código de produção.
 *
 * <p>Exemplo: {@code String rawToken = tokenGenerator.generate();}
 */
public interface OneTimeTokenGenerator {

	/** Gera um token novo, seguro para URL, que só deve aparecer no link enviado por e-mail. */
	String generate();

}
