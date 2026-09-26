package dev.vinicius.cursos.api.passwordreset.repository;

import dev.vinicius.cursos.api.passwordreset.domain.PasswordResetRequest;
import java.time.Instant;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Fila durável de pedidos de recuperação. */
public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, UUID> {

	/**
	 * Trava o próximo pedido pendente e vencido. {@code SKIP LOCKED} faz workers concorrentes (outra thread
	 * ou outra instância) pegarem pedidos diferentes, então cada pedido é processado por um só.
	 *
	 * <p>Exemplo: {@code requestRepository.lockNextDue(clock.instant())}.
	 */
	@Query(value = """
			SELECT * FROM password_reset_requests
			WHERE status = 'PENDING' AND next_attempt_at <= :now
			ORDER BY next_attempt_at
			LIMIT 1
			FOR UPDATE SKIP LOCKED""", nativeQuery = true)
	Optional<PasswordResetRequest> lockNextDue(@Param("now") Instant now);

	/**
	 * Pedido pelo ID, travado até o fim da transação (espera outro worker que o esteja processando).
	 *
	 * <p>Exemplo: {@code requestRepository.lockById(requestId)}.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from PasswordResetRequest r where r.id = :id")
	Optional<PasswordResetRequest> lockById(@Param("id") UUID requestId);

	/**
	 * Apaga pedidos concluídos antes do instante informado; pendentes nunca são apagados.
	 *
	 * <p>Exemplo: {@code requestRepository.deleteCompletedBefore(now.minus(retention))}.
	 */
	@Modifying
	@Query("delete from PasswordResetRequest r where r.completedAt < :threshold")
	int deleteCompletedBefore(@Param("threshold") Instant threshold);

}
