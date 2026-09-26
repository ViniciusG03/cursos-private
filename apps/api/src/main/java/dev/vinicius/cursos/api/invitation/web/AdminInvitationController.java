package dev.vinicius.cursos.api.invitation.web;

import dev.vinicius.cursos.api.invitation.service.InvitationService;
import dev.vinicius.cursos.api.invitation.service.InvitationView;
import dev.vinicius.cursos.api.security.LibraryUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Convites pelo administrador. O papel ADMIN é exigido pela cadeia de segurança em /api/admin/**. */
@RestController
@RequestMapping("/api/admin/invitations")
class AdminInvitationController {

	private final InvitationService invitationService;

	AdminInvitationController(InvitationService invitationService) {
		this.invitationService = invitationService;
	}

	/**
	 * Emite (ou reenvia) o convite e devolve o estado do envio; {@code FAILED} pede novo POST.
	 *
	 * <p>Exemplo: {@code POST /api/admin/invitations {"email":"ana@x.com"} -> 201 {"deliveryStatus":"SENT",...}}.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	InvitationView issue(@Valid @RequestBody IssueInvitationRequest request,
			@AuthenticationPrincipal LibraryUserPrincipal admin) {
		return invitationService.issue(request.email(), admin.userId());
	}

	/**
	 * Convites em aberto, para acompanhar envios e falhas.
	 *
	 * <p>Exemplo: {@code GET /api/admin/invitations -> 200 [{"email":"ana@x.com",...}]}.
	 */
	@GetMapping
	List<InvitationView> listOpen() {
		return invitationService.listOpen();
	}

	/** Corpo do convite. */
	record IssueInvitationRequest(@NotBlank @Size(max = 320) String email) {}

}
