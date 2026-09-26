package dev.vinicius.cursos.api.account.repository;

import dev.vinicius.cursos.api.account.domain.UserAccount;
import dev.vinicius.cursos.api.account.domain.UserRole;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistência de contas. Buscas por e-mail recebem sempre o valor já normalizado. */
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

	/**
	 * Conta com o e-mail normalizado informado.
	 *
	 * <p>Exemplo: {@code accountRepository.findByEmail(AccountEmail.parse(raw).value())}.
	 */
	Optional<UserAccount> findByEmail(String normalizedEmail);

	/**
	 * Conta com o e-mail informado, travada ({@code SELECT ... FOR UPDATE}) até o fim da transação. Serializa
	 * emissões de token de recuperação da mesma conta feitas por workers concorrentes.
	 *
	 * <p>Exemplo: {@code accountRepository.lockByEmail(email.value())}.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from UserAccount a where a.email = :email")
	Optional<UserAccount> lockByEmail(@Param("email") String normalizedEmail);

	/**
	 * Conta com o ID informado, travada até o fim da transação. Ordem de locks do projeto na recuperação:
	 * {@code password_reset_requests} → {@code users} → {@code password_reset_tokens}; quem precisa da conta
	 * e de um token trava a conta primeiro.
	 *
	 * <p>Exemplo: {@code accountRepository.lockById(resetToken.getUserId())}.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from UserAccount a where a.id = :id")
	Optional<UserAccount> lockById(@Param("id") UUID userId);

	/**
	 * Indica se o e-mail normalizado já pertence a uma conta ativa.
	 *
	 * <p>Exemplo: {@code accountRepository.existsByEmail(email.value())}.
	 */
	boolean existsByEmail(String normalizedEmail);

	/**
	 * Primeira conta com o papel informado; na V1 existe no máximo um ADMIN.
	 *
	 * <p>Exemplo: {@code accountRepository.findFirstByRole(UserRole.ADMIN)}.
	 */
	Optional<UserAccount> findFirstByRole(UserRole role);

}
