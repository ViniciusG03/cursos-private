package dev.vinicius.cursos.api.onetimetoken;

import dev.vinicius.cursos.api.support.Sha256Hex;

/**
 * SHA-256 em hexadecimal minúsculo do token bruto. É o único formato do token que chega ao banco,
 * e as buscas comparam por ele: quem lê o banco não consegue usar um convite ou recuperação.
 */
public final class OneTimeTokenDigest {

	private OneTimeTokenDigest() {}

	/**
	 * Calcula o hash usado em {@code token_hash}. Não há salt porque o token já tem 256 bits de entropia.
	 * Token ausente é tratado como token inválido, com a mesma resposta genérica.
	 *
	 * <p>Exemplo: {@code String tokenHash = OneTimeTokenDigest.sha256Hex(rawToken);}
	 */
	public static String sha256Hex(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new InvalidOneTimeTokenException();
		}
		return Sha256Hex.of(rawToken);
	}

}
