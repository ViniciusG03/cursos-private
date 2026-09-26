package dev.vinicius.cursos.api.invitation.service;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.invitation.repository.InvitationRepository;
import dev.vinicius.cursos.api.mail.AccessMailComposer;
import dev.vinicius.cursos.api.mail.MailDeliveryAttempt;
import dev.vinicius.cursos.api.mail.OutboundMail;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Emissão e reenvio de convites pelo administrador, com o envio do e-mail e seu resultado. */
@Service
public class InvitationService {

	private final InvitationRegistry invitationRegistry;

	private final InvitationRepository invitationRepository;

	private final AccessMailComposer mailComposer;

	private final MailDeliveryAttempt deliveryAttempt;

	InvitationService(InvitationRegistry invitationRegistry, InvitationRepository invitationRepository,
			AccessMailComposer mailComposer, MailDeliveryAttempt deliveryAttempt) {
		this.invitationRegistry = invitationRegistry;
		this.invitationRepository = invitationRepository;
		this.mailComposer = mailComposer;
		this.deliveryAttempt = deliveryAttempt;
	}

	/**
	 * Emite o convite (revogando o anterior do mesmo e-mail, o que também serve de reenvio), envia o
	 * e-mail e devolve o estado do envio. Uma falha de SMTP resulta em {@code FAILED}, não em exceção.
	 *
	 * <p>Exemplo: {@code InvitationView view = invitationService.issue("ana@x.com", adminId);}
	 */
	public InvitationView issue(String rawEmail, UUID adminUserId) {
		IssuedInvitation issued = replaceOpenInvitation(AccountEmail.parse(rawEmail), adminUserId);
		OutboundMail mail = mailComposer.invitation(issued.email(), issued.rawToken(), issued.expiresAt());
		boolean delivered = deliveryAttempt.tryDeliver(mail, "invitation", issued.invitationId()).delivered();
		return invitationRegistry.recordDelivery(issued.invitationId(), delivered);
	}

	/**
	 * Convites em aberto (pendentes, enviados, com falha ou expirados sem reenvio), mais novos primeiro.
	 *
	 * <p>Exemplo: {@code invitationService.listOpen().getFirst().deliveryStatus()}.
	 */
	@Transactional(readOnly = true)
	public List<InvitationView> listOpen() {
		return invitationRepository.findOpenNewestFirst().stream().map(InvitationView::of).toList();
	}

	// Dois pedidos simultâneos para o mesmo e-mail: o banco (uq_invitations_open_email) deixa passar um só.
	private IssuedInvitation replaceOpenInvitation(AccountEmail email, UUID adminUserId) {
		try {
			return invitationRegistry.replaceOpenInvitation(email, adminUserId);
		}
		catch (DataIntegrityViolationException concurrentIssue) {
			throw new InvitationConflictException(
					"cannot invite %s: expected no concurrent invitation, got one being issued".formatted(email));
		}
	}

}
