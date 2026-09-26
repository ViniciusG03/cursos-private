package dev.vinicius.cursos.api.passwordreset.web;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.passwordreset.service.PasswordResetConfirmation;
import dev.vinicius.cursos.api.passwordreset.service.PasswordResetRequestService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Recuperação pública de acesso (com CSRF): pedido do link e confirmação da nova senha. */
@RestController
@RequestMapping("/api/auth/password-resets")
class PasswordResetController {

	private final PasswordResetRequestService resetRequestService;

	private final PasswordResetConfirmation resetConfirmation;

	private final AttemptLimiter attemptLimiter;

	PasswordResetController(PasswordResetRequestService resetRequestService,
			PasswordResetConfirmation resetConfirmation, AttemptLimiter attemptLimiter) {
		this.resetRequestService = resetRequestService;
		this.resetConfirmation = resetConfirmation;
		this.attemptLimiter = attemptLimiter;
	}

	/**
	 * Sempre 202 sem corpo para e-mail bem formado, exista a conta ou não.
	 *
	 * <p>Exemplo: {@code POST /api/auth/password-resets/request {"email":"ana@x.com"} -> 202}.
	 */
	@PostMapping("/request")
	@ResponseStatus(HttpStatus.ACCEPTED)
	void request(@Valid @RequestBody PasswordResetRequest request, HttpServletRequest httpRequest) {
		AccountEmail email = AccountEmail.parse(request.email());
		attemptLimiter.recordAttempt(AttemptScope.PASSWORD_RESET_REQUEST, ClientAddress.of(httpRequest),
				email.value());
		resetRequestService.requestReset(email);
	}

	/**
	 * Consome o token do link e troca a senha; sessões antigas deixam de valer. Não cria sessão.
	 *
	 * <p>Exemplo: {@code POST /api/auth/password-resets/confirm {"token":"...","password":"..."} -> 204}.
	 */
	@PostMapping("/confirm")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void confirm(@Valid @RequestBody PasswordResetConfirmRequest request, HttpServletRequest httpRequest) {
		attemptLimiter.recordAttempt(AttemptScope.PASSWORD_RESET_CONFIRM, ClientAddress.of(httpRequest), null);
		resetConfirmation.confirm(request.token(), request.password());
	}

	/** Corpo do pedido de recuperação. */
	record PasswordResetRequest(@NotBlank @Size(max = 320) String email) {}

	/** Token do link e nova senha; {@link #toString()} omite os dois. */
	record PasswordResetConfirmRequest(@NotBlank @Size(max = 256) String token,
			@NotNull @Size(max = 1024) String password) {

		@Override
		public String toString() {
			return "PasswordResetConfirmRequest[token=<omitted>, password=<omitted>]";
		}

	}

}
