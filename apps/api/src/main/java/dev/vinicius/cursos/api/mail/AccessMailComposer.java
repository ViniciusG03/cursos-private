package dev.vinicius.cursos.api.mail;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.config.PublicOrigin;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Textos dos e-mails de convite e recuperação. O token vai no fragmento ({@code #token=...}) do link:
 * o navegador não o envia ao servidor nem no cabeçalho {@code Referer}, então ele não aparece em logs
 * de acesso do Nginx nem em sites externos abertos a partir das telas do marco 3.
 */
@Component
public class AccessMailComposer {

	static final String INVITATION_PATH = "/convites/aceitar";

	static final String PASSWORD_RESET_PATH = "/recuperar-acesso/nova-senha";

	private final PublicOrigin publicOrigin;

	AccessMailComposer(PublicOrigin publicOrigin) {
		this.publicOrigin = publicOrigin;
	}

	/**
	 * Monta o convite com o link de aceite e a validade.
	 *
	 * <p>Exemplo: {@code composer.invitation(email, rawToken, invitation.getExpiresAt())}.
	 */
	public OutboundMail invitation(AccountEmail recipient, String rawToken, Instant expiresAt) {
		String body = """
				Você foi convidado para a biblioteca de cursos.

				Para definir sua senha e entrar, acesse:
				%s

				O link vale até %s (UTC) e só pode ser usado uma vez.
				Se você não esperava este convite, ignore esta mensagem.
				""".formatted(publicOrigin.linkWithToken(INVITATION_PATH, rawToken), expiresAt);
		return new OutboundMail(recipient, "Convite para a biblioteca de cursos", body);
	}

	/**
	 * Monta a recuperação de acesso com o link de nova senha e a validade.
	 *
	 * <p>Exemplo: {@code composer.passwordReset(email, rawToken, resetToken.getExpiresAt())}.
	 */
	public OutboundMail passwordReset(AccountEmail recipient, String rawToken, Instant expiresAt) {
		String body = """
				Recebemos um pedido para redefinir sua senha na biblioteca de cursos.

				Para escolher uma nova senha, acesse:
				%s

				O link vale até %s (UTC) e só pode ser usado uma vez.
				Se você não fez este pedido, ignore esta mensagem: sua senha continua a mesma.
				""".formatted(publicOrigin.linkWithToken(PASSWORD_RESET_PATH, rawToken), expiresAt);
		return new OutboundMail(recipient, "Recuperação de acesso à biblioteca de cursos", body);
	}

}
