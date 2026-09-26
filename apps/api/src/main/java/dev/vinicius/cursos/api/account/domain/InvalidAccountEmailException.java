package dev.vinicius.cursos.api.account.domain;

/**
 * E-mail ausente, sem formato plausível ou maior que a coluna.
 *
 * <p>Exemplo: {@code throw new InvalidAccountEmailException("email must be like ..., got: 'ana'")}.
 */
public class InvalidAccountEmailException extends IllegalArgumentException {

	public InvalidAccountEmailException(String message) {
		super(message);
	}

}
