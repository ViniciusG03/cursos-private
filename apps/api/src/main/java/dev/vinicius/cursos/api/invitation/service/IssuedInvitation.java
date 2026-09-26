package dev.vinicius.cursos.api.invitation.service;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import java.time.Instant;
import java.util.UUID;

/**
 * Convite recém-gravado junto com o token bruto, que só existe em memória até virar link no e-mail.
 * {@link #toString()} omite o token para que um log acidental não o exponha.
 */
record IssuedInvitation(UUID invitationId, AccountEmail email, String rawToken, Instant expiresAt) {

	@Override
	public String toString() {
		return "IssuedInvitation[invitationId=%s, email=%s, rawToken=<omitted>, expiresAt=%s]".formatted(invitationId,
				email, expiresAt);
	}

}
