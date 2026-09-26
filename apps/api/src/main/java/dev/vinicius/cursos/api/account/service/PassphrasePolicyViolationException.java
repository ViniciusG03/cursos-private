package dev.vinicius.cursos.api.account.service;

/**
 * Senha fora da política. A mensagem nunca contém a senha recebida.
 *
 * <p>Exemplo: {@code throw new PassphrasePolicyViolationException("password must have ..., got 5")}.
 */
public class PassphrasePolicyViolationException extends IllegalArgumentException {

	public PassphrasePolicyViolationException(String message) {
		super(message);
	}

}
