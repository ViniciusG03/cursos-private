package dev.vinicius.cursos.api.mail;

import dev.vinicius.cursos.api.account.domain.AccountEmail;

/**
 * Mensagem de texto simples para um único destinatário. O corpo carrega links com token, por isso
 * {@link #toString()} o omite: um log acidental da mensagem não pode vazar o token.
 *
 * <p>Exemplo: {@code new OutboundMail(email, "Convite", "Acesse: https://...")}.
 */
public record OutboundMail(AccountEmail recipient, String subject, String body) {

	@Override
	public String toString() {
		return "OutboundMail[recipient=%s, subject=%s, body=<omitted>]".formatted(recipient, subject);
	}

}
