package dev.vinicius.cursos.api.passwordreset.service;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.passwordreset.domain.PasswordResetRequest;
import dev.vinicius.cursos.api.passwordreset.repository.PasswordResetRequestRepository;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pedido público de recuperação de acesso. Só grava o pedido na fila durável e retorna: o mesmo INSERT
 * acontece com ou sem conta, então resposta e tempo não revelam se o e-mail existe. A busca da conta, a
 * emissão do token e o envio ficam com o {@link PasswordResetDispatcher}; se o processo parar depois do
 * 202, o pedido continua no banco e é atendido quando a aplicação voltar.
 */
@Service
public class PasswordResetRequestService {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetRequestService.class);

	private final PasswordResetRequestRepository requestRepository;

	private final Clock clock;

	PasswordResetRequestService(PasswordResetRequestRepository requestRepository, Clock clock) {
		this.requestRepository = requestRepository;
		this.clock = clock;
	}

	/**
	 * Enfileira o pedido e devolve o ID dele (para logs; nunca vai para a resposta pública).
	 *
	 * <p>Exemplo: {@code resetRequestService.requestReset(AccountEmail.parse("ana@x.com"));}
	 */
	@Transactional
	public UUID requestReset(AccountEmail email) {
		UUID requestId = requestRepository.save(PasswordResetRequest.queue(email, clock.instant())).getId();
		log.atInfo().addKeyValue("requestId", requestId).log("password reset request queued");
		return requestId;
	}

}
