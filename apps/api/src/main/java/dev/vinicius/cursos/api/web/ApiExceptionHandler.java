package dev.vinicius.cursos.api.web;

import dev.vinicius.cursos.api.account.domain.InvalidAccountEmailException;
import dev.vinicius.cursos.api.account.service.PassphrasePolicyViolationException;
import dev.vinicius.cursos.api.catalog.domain.InvalidCatalogTitleException;
import dev.vinicius.cursos.api.catalog.service.CatalogItemNotFoundException;
import dev.vinicius.cursos.api.catalog.service.CourseNotPublishableException;
import dev.vinicius.cursos.api.catalog.service.InvalidReorderException;
import dev.vinicius.cursos.api.catalog.service.PublishedCourseModificationException;
import dev.vinicius.cursos.api.invitation.service.InvitationConflictException;
import dev.vinicius.cursos.api.onetimetoken.InvalidOneTimeTokenException;
import dev.vinicius.cursos.api.security.attemptlimit.TooManyAttemptsException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Converte exceções de domínio em {@code application/problem+json}. Só repassa mensagens escritas por
 * este projeto, que nunca contêm senha, token ou hash; erros do framework ganham texto fixo, porque o
 * Jackson pode ecoar trechos do corpo (e o corpo pode conter uma senha).
 */
@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(CatalogItemNotFoundException.class)
	ProblemDetail notFound(CatalogItemNotFoundException notFound) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, notFound.getMessage());
	}

	@ExceptionHandler({ InvalidCatalogTitleException.class, InvalidReorderException.class,
			InvalidAccountEmailException.class, PassphrasePolicyViolationException.class,
			InvalidOneTimeTokenException.class })
	ProblemDetail badRequest(RuntimeException invalidInput) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, invalidInput.getMessage());
	}

	@ExceptionHandler({ CourseNotPublishableException.class, PublishedCourseModificationException.class,
			InvitationConflictException.class })
	ProblemDetail conflict(RuntimeException conflictingState) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, conflictingState.getMessage());
	}

	// A mensagem do PostgreSQL cita valores de chave (ex.: e-mail); fica fora da resposta.
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail constraintViolation(DataIntegrityViolationException violation) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"conflicting change: expected the resource to be in its current state, got a concurrent update");
	}

	@ExceptionHandler(TooManyAttemptsException.class)
	ResponseEntity<ProblemDetail> tooManyAttempts(TooManyAttemptsException tooManyAttempts) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
			.header(HttpHeaders.RETRY_AFTER, String.valueOf(tooManyAttempts.retryAfterSeconds()))
			.body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, tooManyAttempts.getMessage()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail unreadableBody(HttpMessageNotReadableException unreadable) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"request body must be JSON matching this endpoint's documented fields");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail invalidFields(MethodArgumentNotValidException invalid) {
		List<String> invalidFields = invalid.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
			.sorted()
			.toList();
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"invalid request fields (values omitted): " + invalidFields);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail invalidPathValue(MethodArgumentTypeMismatchException mismatch) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"parameter '%s' must be a valid %s".formatted(mismatch.getName(),
						mismatch.getRequiredType() == null ? "value" : mismatch.getRequiredType().getSimpleName()));
	}

}
