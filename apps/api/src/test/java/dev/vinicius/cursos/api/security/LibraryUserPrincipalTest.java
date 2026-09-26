package dev.vinicius.cursos.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.account.domain.UserAccount;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LibraryUserPrincipalTest {

	private static final String PASSWORD_HASH = "{pbkdf2@SpringSecurity_v5_8}abc123";

	private final UserAccount member = UserAccount.member(AccountEmail.parse("ana@x.com"), PASSWORD_HASH,
			Instant.parse("2026-01-05T12:00:00Z"));

	@Test
	void eraseCredentialsRemovesHashBeforeStoringInSession() {
		LibraryUserPrincipal principal = LibraryUserPrincipal.forAuthentication(member);

		principal.eraseCredentials();

		assertThat(principal.getPassword()).isNull();
	}

	@Test
	void survivesSessionSerializationWithRoleAndVersion() throws Exception {
		LibraryUserPrincipal principal = LibraryUserPrincipal.authenticated(member);

		LibraryUserPrincipal restored = roundTrip(principal);

		assertThat(restored.getUsername()).isEqualTo("ana@x.com");
		assertThat(restored.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_MEMBER");
		assertThat(restored.credentialVersion()).isZero();
	}

	@Test
	void toStringNeverShowsHash() {
		assertThat(LibraryUserPrincipal.forAuthentication(member).toString()).doesNotContain(PASSWORD_HASH);
	}

	private static LibraryUserPrincipal roundTrip(LibraryUserPrincipal principal) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
			output.writeObject(principal);
		}
		try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			return (LibraryUserPrincipal) input.readObject();
		}
	}

}
