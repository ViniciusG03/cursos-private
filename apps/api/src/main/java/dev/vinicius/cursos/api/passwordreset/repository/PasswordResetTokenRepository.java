package dev.vinicius.cursos.api.passwordreset.repository;

import dev.vinicius.cursos.api.passwordreset.domain.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistência de tokens de recuperação. Buscas por token recebem sempre o hash. */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

	/**
	 * Busca pelo hash com {@code SELECT ... FOR UPDATE}, para que duas confirmações simultâneas não usem
	 * o mesmo token.
	 *
	 * <p>Exemplo: {@code resetTokenRepository.lockByTokenHash(OneTimeTokenDigest.sha256Hex(raw))}.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from PasswordResetToken t where t.tokenHash = :tokenHash")
	Optional<PasswordResetToken> lockByTokenHash(@Param("tokenHash") String tokenHash);

	/**
	 * Conta dona do token com o hash informado, sem lock: serve para travar a conta antes do token.
	 *
	 * <p>Exemplo: {@code resetTokenRepository.findUserIdByTokenHash(tokenHash)}.
	 */
	@Query("select t.userId from PasswordResetToken t where t.tokenHash = :tokenHash")
	Optional<UUID> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

	/**
	 * Revoga tokens ainda utilizáveis da conta. UPDATE em lote para rodar antes do INSERT do token novo
	 * (ver {@code uq_password_reset_tokens_open_user}). Não limpa o contexto de persistência: roda na
	 * transação do worker, que mantém o pedido travado e gerenciado; limpar o contexto descartava a marcação
	 * {@code SENT} e o pedido era reenviado a cada varredura.
	 *
	 * <p>Exemplo: {@code resetTokenRepository.revokeOpenForUser(userId, now)}.
	 */
	@Modifying(flushAutomatically = true)
	@Query("""
			update PasswordResetToken t set t.revokedAt = :now
			where t.userId = :userId and t.consumedAt is null and t.revokedAt is null""")
	int revokeOpenForUser(@Param("userId") UUID userId, @Param("now") Instant now);

}
