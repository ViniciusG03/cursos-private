package dev.vinicius.cursos.api.security;

import dev.vinicius.cursos.api.account.domain.UserAccount;
import dev.vinicius.cursos.api.account.domain.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal guardado na sessão. Leva a {@code credential_version} do momento do login, que o
 * {@link SessionCredentialGuardFilter} compara com o banco a cada requisição. O hash da senha só
 * existe durante a autenticação: o Spring Security chama {@link #eraseCredentials()} logo depois.
 */
public final class LibraryUserPrincipal implements UserDetails, CredentialsContainer {

	private final UUID userId;

	private final String email;

	private final UserRole role;

	private final long credentialVersion;

	private String passwordHash;

	private LibraryUserPrincipal(UUID userId, String email, UserRole role, long credentialVersion,
			String passwordHash) {
		this.userId = userId;
		this.email = email;
		this.role = role;
		this.credentialVersion = credentialVersion;
		this.passwordHash = passwordHash;
	}

	/**
	 * Principal para autenticação por senha, com o hash ainda presente.
	 *
	 * <p>Exemplo: {@code LibraryUserPrincipal.forAuthentication(account)}.
	 */
	public static LibraryUserPrincipal forAuthentication(UserAccount account) {
		return new LibraryUserPrincipal(account.getId(), account.getEmail().value(), account.getRole(),
				account.getCredentialVersion(), account.getPasswordHash());
	}

	/**
	 * Principal já autenticado, sem hash, com papel e versão lidos agora do banco.
	 *
	 * <p>Exemplo: {@code LibraryUserPrincipal.authenticated(accountRepository.findById(id).orElseThrow())}.
	 */
	public static LibraryUserPrincipal authenticated(UserAccount account) {
		return new LibraryUserPrincipal(account.getId(), account.getEmail().value(), account.getRole(),
				account.getCredentialVersion(), null);
	}

	public UUID userId() {
		return userId;
	}

	public UserRole role() {
		return role;
	}

	public long credentialVersion() {
		return credentialVersion;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(role.authority()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public void eraseCredentials() {
		this.passwordHash = null;
	}

	@Override
	public String toString() {
		return "LibraryUserPrincipal[userId=%s, role=%s, credentialVersion=%d]".formatted(userId, role,
				credentialVersion);
	}

}
