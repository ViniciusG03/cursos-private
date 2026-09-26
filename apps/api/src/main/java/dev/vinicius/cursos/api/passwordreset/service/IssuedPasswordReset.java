package dev.vinicius.cursos.api.passwordreset.service;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import java.time.Instant;
import java.util.UUID;

/** Token de recuperação recém-gravado e seu valor bruto; {@link #toString()} omite o token. */
record IssuedPasswordReset(UUID resetTokenId, AccountEmail email, String rawToken, Instant expiresAt) {

	@Override
	public String toString() {
		return "IssuedPasswordReset[resetTokenId=%s, email=%s, rawToken=<omitted>, expiresAt=%s]"
			.formatted(resetTokenId, email, expiresAt);
	}

}
