package dev.vinicius.cursos.api.security.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Escreve respostas JSON a partir dos filtros de segurança, que rodam antes do Spring MVC e não passam
 * pelo {@code @RestControllerAdvice}. Mantém o mesmo formato {@code application/problem+json} da API.
 */
@Component
public class ProblemResponseWriter {

	private final JsonMapper jsonMapper;

	ProblemResponseWriter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	/**
	 * Escreve um problem detail com status e detalhe fixos (nunca com dados vindos da requisição).
	 *
	 * <p>Exemplo: {@code problemWriter.writeProblem(response, HttpStatus.UNAUTHORIZED, "authentication required");}
	 */
	public void writeProblem(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
		Map<String, Object> problem = new LinkedHashMap<>();
		problem.put("type", "about:blank");
		problem.put("title", status.getReasonPhrase());
		problem.put("status", status.value());
		problem.put("detail", detail);
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		jsonMapper.writeValue(response.getOutputStream(), problem);
	}

	/**
	 * Escreve um corpo JSON comum com o status informado.
	 *
	 * <p>Exemplo: {@code problemWriter.writeJson(response, HttpStatus.OK, CurrentUserView.of(principal));}
	 */
	public void writeJson(HttpServletResponse response, HttpStatus status, Object body) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		jsonMapper.writeValue(response.getOutputStream(), body);
	}

}
