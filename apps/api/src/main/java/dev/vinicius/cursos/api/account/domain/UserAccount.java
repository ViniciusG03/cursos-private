package dev.vinicius.cursos.api.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Conta da biblioteca, mapeada sobre {@code users} (V4). Guarda somente o hash da senha; a senha em
 * texto nunca chega a esta classe.
 */
@Entity
@Table(name = "users")
public class UserAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "email", nullable = false, length = AccountEmail.MAX_LENGTH)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private UserRole role;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "password_changed_at", nullable = false)
	private Instant passwordChangedAt;

	@Column(name = "credential_version", nullable = false)
	private long credentialVersion;

	protected UserAccount() {}

	private UserAccount(AccountEmail email, String passwordHash, UserRole role, Instant now) {
		this.email = email.value();
		this.passwordHash = passwordHash;
		this.role = role;
		this.createdAt = now;
		this.passwordChangedAt = now;
		this.credentialVersion = 0;
	}

	/**
	 * Cria o administrador inicial; usado apenas pelo bootstrap explícito.
	 *
	 * <p>Exemplo: {@code UserAccount.admin(AccountEmail.parse("adm@x.com"), hash, now)}.
	 */
	public static UserAccount admin(AccountEmail email, String passwordHash, Instant now) {
		return new UserAccount(email, passwordHash, UserRole.ADMIN, now);
	}

	/**
	 * Cria um membro no aceite do convite, já com o hash da senha escolhida por ele.
	 *
	 * <p>Exemplo: {@code UserAccount.member(invitation.email(), hash, now)}.
	 */
	public static UserAccount member(AccountEmail email, String passwordHash, Instant now) {
		return new UserAccount(email, passwordHash, UserRole.MEMBER, now);
	}

	/**
	 * Troca o hash da senha e avança {@code credential_version}, o que invalida as sessões abertas antes.
	 *
	 * <p>Exemplo: {@code account.replacePasswordHash(newHash, now);}
	 */
	public void replacePasswordHash(String newPasswordHash, Instant now) {
		this.passwordHash = newPasswordHash;
		this.passwordChangedAt = now;
		this.credentialVersion++;
	}

	public UUID getId() {
		return id;
	}

	public AccountEmail getEmail() {
		return new AccountEmail(email);
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public UserRole getRole() {
		return role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getPasswordChangedAt() {
		return passwordChangedAt;
	}

	public long getCredentialVersion() {
		return credentialVersion;
	}

}
