package dev.vinicius.cursos.api.invitation.repository;

import dev.vinicius.cursos.api.invitation.domain.Invitation;
import dev.vinicius.cursos.api.invitation.domain.InvitationDeliveryStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistência de convites. Buscas por token recebem sempre o hash, nunca o token bruto. */
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

	/**
	 * Busca o convite pelo hash com {@code SELECT ... FOR UPDATE}: um segundo aceite simultâneo espera o
	 * primeiro terminar e então enxerga o convite já consumido.
	 *
	 * <p>Exemplo: {@code invitationRepository.lockByTokenHash(OneTimeTokenDigest.sha256Hex(raw))}.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from Invitation i where i.tokenHash = :tokenHash")
	Optional<Invitation> lockByTokenHash(@Param("tokenHash") String tokenHash);

	/**
	 * E-mail do convite com o hash informado, sem lock; usado para pegar o lock por e-mail antes do lock
	 * da linha.
	 *
	 * <p>Exemplo: {@code invitationRepository.findEmailByTokenHash(tokenHash)}.
	 */
	@Query("select i.email from Invitation i where i.tokenHash = :tokenHash")
	Optional<String> findEmailByTokenHash(@Param("tokenHash") String tokenHash);

	/**
	 * Revoga os convites em aberto do e-mail. É um UPDATE em lote de propósito: o Hibernate executa
	 * INSERTs antes de UPDATEs no flush, e o convite novo colidiria com {@code uq_invitations_open_email}.
	 *
	 * <p>Exemplo: {@code invitationRepository.revokeOpenForEmail(email.value(), now)}.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Invitation i set i.revokedAt = :now
			where i.email = :email and i.consumedAt is null and i.revokedAt is null""")
	int revokeOpenForEmail(@Param("email") String normalizedEmail, @Param("now") Instant now);

	/**
	 * Grava só as colunas de envio. Um UPDATE da entidade inteira regravaria o {@code revoked_at} lido
	 * antes de um reenvio concorrente e ressuscitaria o convite já revogado.
	 *
	 * <p>Exemplo: {@code invitationRepository.recordDelivery(id, InvitationDeliveryStatus.SENT, now, now)}.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Invitation i set i.deliveryStatus = :status, i.deliveryAttemptedAt = :attemptedAt,
			    i.deliveredAt = :deliveredAt
			where i.id = :id""")
	int recordDelivery(@Param("id") UUID invitationId, @Param("status") InvitationDeliveryStatus status,
			@Param("attemptedAt") Instant attemptedAt, @Param("deliveredAt") Instant deliveredAt);

	/**
	 * Convites ainda não consumidos nem revogados (inclusive expirados), do mais novo para o mais antigo.
	 *
	 * <p>Exemplo: {@code invitationRepository.findOpenNewestFirst()}.
	 */
	@Query("""
			select i from Invitation i
			where i.consumedAt is null and i.revokedAt is null
			order by i.createdAt desc""")
	List<Invitation> findOpenNewestFirst();

}
