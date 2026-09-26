package dev.vinicius.cursos.api.invitation.service;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.account.repository.UserAccountRepository;
import dev.vinicius.cursos.api.invitation.domain.Invitation;
import dev.vinicius.cursos.api.invitation.domain.InvitationDeliveryStatus;
import dev.vinicius.cursos.api.invitation.repository.InvitationEmailLock;
import dev.vinicius.cursos.api.invitation.repository.InvitationRepository;
import dev.vinicius.cursos.api.onetimetoken.OneTimeTokenDigest;
import dev.vinicius.cursos.api.onetimetoken.OneTimeTokenGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transações curtas do ciclo do convite. Ficam separadas de {@link InvitationService} para que o
 * envio SMTP (lento e sujeito a falha) aconteça fora de qualquer transação do banco.
 */
@Component
class InvitationRegistry {

	private final InvitationRepository invitationRepository;

	private final UserAccountRepository accountRepository;

	private final InvitationEmailLock invitationEmailLock;

	private final OneTimeTokenGenerator tokenGenerator;

	private final Clock clock;

	InvitationRegistry(InvitationRepository invitationRepository, UserAccountRepository accountRepository,
			InvitationEmailLock invitationEmailLock, OneTimeTokenGenerator tokenGenerator, Clock clock) {
		this.invitationRepository = invitationRepository;
		this.accountRepository = accountRepository;
		this.invitationEmailLock = invitationEmailLock;
		this.tokenGenerator = tokenGenerator;
		this.clock = clock;
	}

	/**
	 * Revoga o convite em aberto do e-mail (se houver) e grava um novo com token inédito. O lock por e-mail
	 * vem antes da checagem de conta: um aceite em andamento termina primeiro e a conta criada por ele já
	 * aparece aqui.
	 *
	 * <p>Exemplo: {@code IssuedInvitation issued = registry.replaceOpenInvitation(email, adminId);}
	 */
	@Transactional
	IssuedInvitation replaceOpenInvitation(AccountEmail email, UUID adminUserId) {
		invitationEmailLock.lockFor(email);
		if (accountRepository.existsByEmail(email.value())) {
			throw new InvitationConflictException(
					"cannot invite %s: expected an email without account, got an active account".formatted(email));
		}
		Instant now = clock.instant();
		invitationRepository.revokeOpenForEmail(email.value(), now);
		String rawToken = tokenGenerator.generate();
		Invitation invitation = Invitation.issue(email, OneTimeTokenDigest.sha256Hex(rawToken), adminUserId, now);
		invitationRepository.saveAndFlush(invitation);
		return new IssuedInvitation(invitation.getId(), email, rawToken, invitation.getExpiresAt());
	}

	/**
	 * Grava o resultado do envio e devolve a visão atualizada para o administrador.
	 *
	 * <p>Exemplo: {@code InvitationView view = registry.recordDelivery(invitationId, true);}
	 */
	@Transactional
	InvitationView recordDelivery(UUID invitationId, boolean delivered) {
		Instant now = clock.instant();
		InvitationDeliveryStatus status = delivered ? InvitationDeliveryStatus.SENT : InvitationDeliveryStatus.FAILED;
		invitationRepository.recordDelivery(invitationId, status, now, delivered ? now : null);
		return invitationRepository.findById(invitationId)
			.map(InvitationView::of)
			.orElseThrow(() -> new IllegalStateException(
					"invitation %s must exist right after issuing, got none".formatted(invitationId)));
	}

}
