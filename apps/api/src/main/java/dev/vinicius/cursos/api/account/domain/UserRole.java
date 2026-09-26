package dev.vinicius.cursos.api.account.domain;

/**
 * Papel da conta, persistido como texto em {@code users.role}. Nunca vem do corpo de uma requisição:
 * ADMIN nasce pelo bootstrap e MEMBER pelo aceite de convite.
 *
 * <p>Exemplo: {@code account.getRole() == UserRole.ADMIN}.
 */
public enum UserRole {
	ADMIN,
	MEMBER;

	/**
	 * Nome da autoridade no Spring Security, no formato esperado por {@code hasRole}.
	 *
	 * <p>Exemplo: {@code UserRole.ADMIN.authority()} devolve {@code "ROLE_ADMIN"}.
	 */
	public String authority() {
		return "ROLE_" + name();
	}
}
