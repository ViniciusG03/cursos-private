package dev.vinicius.cursos.api.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 de um texto UTF-8 em hexadecimal minúsculo (64 caracteres). */
public final class Sha256Hex {

	private Sha256Hex() {}

	/**
	 * Calcula o hash do texto.
	 *
	 * <p>Exemplo: {@code Sha256Hex.of("abc")} devolve {@code "ba7816bf..."}.
	 */
	public static String of(String text) {
		return HexFormat.of().formatHex(sha256().digest(text.getBytes(StandardCharsets.UTF_8)));
	}

	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException missingAlgorithm) {
			throw new IllegalStateException("JVM must provide SHA-256, got none", missingAlgorithm);
		}
	}

}
