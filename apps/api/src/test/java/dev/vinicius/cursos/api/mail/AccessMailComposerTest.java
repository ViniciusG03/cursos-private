package dev.vinicius.cursos.api.mail;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.config.PublicOrigin;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AccessMailComposerTest {

	private static final AccountEmail RECIPIENT = AccountEmail.parse("ana@x.com");

	private static final Instant EXPIRES_AT = Instant.parse("2026-01-08T12:00:00Z");

	private final AccessMailComposer composer = composerFor("https://cursos.example.com/");

	@Test
	void invitationLinkCarriesTokenInFragmentUnderPublicBase() {
		OutboundMail mail = composer.invitation(RECIPIENT, "tok_123", EXPIRES_AT);

		assertThat(mail.recipient()).isEqualTo(RECIPIENT);
		assertThat(mail.body()).contains("https://cursos.example.com/convites/aceitar#token=tok_123")
			.contains("2026-01-08T12:00:00Z");
	}

	@Test
	void passwordResetLinkCarriesTokenInFragment() {
		OutboundMail mail = composer.passwordReset(RECIPIENT, "tok_456", EXPIRES_AT);

		assertThat(mail.body()).contains("https://cursos.example.com/recuperar-acesso/nova-senha#token=tok_456");
	}

	@Test
	void toStringOmitsBodyWithToken() {
		OutboundMail mail = composer.invitation(RECIPIENT, "tok_789", EXPIRES_AT);

		assertThat(mail.toString()).doesNotContain("tok_789").contains("ana@x.com");
	}

	private static AccessMailComposer composerFor(String publicBaseUrl) {
		return new AccessMailComposer(PublicOrigin.parse(URI.create(publicBaseUrl), false));
	}

}
