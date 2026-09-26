package dev.vinicius.cursos.api.security.web;

import dev.vinicius.cursos.api.account.domain.UserRole;
import dev.vinicius.cursos.api.security.LibraryUserPrincipal;
import java.util.UUID;

/**
 * Usuário autenticado como a SPA o vê: ID, e-mail e papel. Nunca inclui hash nem versão da credencial.
 *
 * <p>Exemplo: {@code CurrentUserView.of(principal).role()}.
 */
public record CurrentUserView(UUID id, String email, UserRole role) {

	/**
	 * Converte o principal da requisição.
	 *
	 * <p>Exemplo: {@code CurrentUserView view = CurrentUserView.of(principal);}
	 */
	public static CurrentUserView of(LibraryUserPrincipal principal) {
		return new CurrentUserView(principal.userId(), principal.getUsername(), principal.role());
	}

}
