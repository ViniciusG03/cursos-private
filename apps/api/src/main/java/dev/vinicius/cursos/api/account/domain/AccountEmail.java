package dev.vinicius.cursos.api.account.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * E-mail já normalizado (sem espaços nas pontas e em minúsculas). Toda entrada, busca e unicidade de
 * contas e convites passa por aqui, para que "Ana@X.com " e "ana@x.com" sejam a mesma identidade.
 *
 * <p>Exemplo: {@code AccountEmail email = AccountEmail.parse(" Ana@Example.com ");}
 */
public record AccountEmail(String value) {

	public static final int MAX_LENGTH = 254;

	// Validação deliberadamente simples: o e-mail só é provado de verdade quando o convite chega.
	private static final Pattern PLAUSIBLE_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	public AccountEmail {
		requirePlausible(value);
	}

	/**
	 * Normaliza e valida um e-mail digitado pelo usuário ou pelo administrador.
	 *
	 * <p>Exemplo: {@code AccountEmail.parse("Ana@Example.com").value()} devolve {@code "ana@example.com"}.
	 */
	public static AccountEmail parse(String rawEmail) {
		if (rawEmail == null) {
			throw new InvalidAccountEmailException("email must be like name@example.com, got: null");
		}
		return new AccountEmail(rawEmail.strip().toLowerCase(Locale.ROOT));
	}

	private static void requirePlausible(String normalizedEmail) {
		if (normalizedEmail == null || normalizedEmail.length() > MAX_LENGTH
				|| !PLAUSIBLE_EMAIL.matcher(normalizedEmail).matches()) {
			throw new InvalidAccountEmailException(
					"email must be like name@example.com with at most %d characters, got: '%s'".formatted(MAX_LENGTH,
							normalizedEmail));
		}
		if (!normalizedEmail.equals(normalizedEmail.strip().toLowerCase(Locale.ROOT))) {
			throw new InvalidAccountEmailException(
					"email must be normalized (trimmed, lower case), got: '%s'".formatted(normalizedEmail));
		}
	}

	@Override
	public String toString() {
		return value;
	}

}
