package dev.vinicius.cursos.api.account.service;

/**
 * Política de senha da biblioteca: passphrases de 12 a 128 caracteres (code points, então emojis e
 * acentos contam como um). Nada é truncado nem aparado: o que o usuário digitou é o que vira hash.
 */
public final class PassphrasePolicy {

	public static final int MIN_CODE_POINTS = 12;

	public static final int MAX_CODE_POINTS = 128;

	private PassphrasePolicy() {}

	/**
	 * Rejeita senhas fora do intervalo. A mensagem cita só o tamanho, nunca a senha.
	 *
	 * <p>Exemplo: {@code PassphrasePolicy.requireAcceptable("cavalo correto bateria grampo");}
	 */
	public static void requireAcceptable(String passphrase) {
		if (passphrase == null || passphrase.isBlank()) {
			throw new PassphrasePolicyViolationException(
					"password must have between %d and %d characters, got an empty value".formatted(MIN_CODE_POINTS,
							MAX_CODE_POINTS));
		}
		int codePoints = passphrase.codePointCount(0, passphrase.length());
		if (codePoints < MIN_CODE_POINTS || codePoints > MAX_CODE_POINTS) {
			throw new PassphrasePolicyViolationException("password must have between %d and %d characters, got %d"
				.formatted(MIN_CODE_POINTS, MAX_CODE_POINTS, codePoints));
		}
	}

}
