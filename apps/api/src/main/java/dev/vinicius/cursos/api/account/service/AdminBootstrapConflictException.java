package dev.vinicius.cursos.api.account.service;

/**
 * Bootstrap pedido para um e-mail que não bate com o administrador existente, ou que já é de um membro.
 * Nada é alterado: o operador precisa resolver a divergência conscientemente.
 *
 * <p>Exemplo: {@code throw new AdminBootstrapConflictException("admin already exists as a@x.com, got b@x.com")}.
 */
public class AdminBootstrapConflictException extends RuntimeException {

	public AdminBootstrapConflictException(String message) {
		super(message);
	}

}
