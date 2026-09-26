package dev.vinicius.cursos.api.invitation.web;

import dev.vinicius.cursos.api.invitation.service.InvitationAcceptance;
import dev.vinicius.cursos.api.security.attemptlimit.AttemptLimiter;
import dev.vinicius.cursos.api.security.attemptlimit.AttemptScope;
import dev.vinicius.cursos.api.security.web.ClientAddress;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Aceite público de convite (com CSRF). É a única forma de nascer uma conta MEMBER. */
@RestController
class InvitationAcceptanceController {

	private final InvitationAcceptance invitationAcceptance;

	private final AttemptLimiter attemptLimiter;

	InvitationAcceptanceController(InvitationAcceptance invitationAcceptance, AttemptLimiter attemptLimiter) {
		this.invitationAcceptance = invitationAcceptance;
		this.attemptLimiter = attemptLimiter;
	}

	/**
	 * Consome o token do link e define a senha do convidado; não cria sessão.
	 *
	 * <p>Exemplo: {@code POST /api/auth/invitations/accept {"token":"...","password":"..."} -> 204}.
	 */
	@PostMapping("/api/auth/invitations/accept")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void accept(@Valid @RequestBody AcceptInvitationRequest request, HttpServletRequest httpRequest) {
		attemptLimiter.recordAttempt(AttemptScope.INVITATION_ACCEPT, ClientAddress.of(httpRequest), null);
		invitationAcceptance.accept(request.token(), request.password());
	}

	/** Token do link e senha escolhida; {@link #toString()} omite os dois. */
	record AcceptInvitationRequest(@NotBlank @Size(max = 256) String token,
			@NotNull @Size(max = 1024) String password) {

		@Override
		public String toString() {
			return "AcceptInvitationRequest[token=<omitted>, password=<omitted>]";
		}

	}

}
