package dev.vinicius.cursos.api.onetimetoken;

/**
 * Token de convite ou recuperação desconhecido, expirado, revogado ou já usado. A mensagem é a mesma
 * em todos os casos e nunca inclui o token: ela pode chegar ao corpo de uma resposta pública.
 *
 * <p>Exemplo: {@code throw new InvalidOneTimeTokenException();}
 */
public class InvalidOneTimeTokenException extends RuntimeException {

	public InvalidOneTimeTokenException() {
		super("token is invalid or expired: expected an unused token from a recent e-mail link");
	}

}
