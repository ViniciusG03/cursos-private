package dev.vinicius.cursos.api.invitation.repository;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import java.sql.ResultSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serializa, por e-mail, as operações que decidem se um convite pode existir: emitir/reenviar e aceitar.
 * Usa {@code pg_advisory_xact_lock}, liberado no commit ou rollback. Sem isso, um reenvio podia checar
 * "sem conta" enquanto um aceite ainda não confirmado criava a conta, e emitir um convite para quem já
 * é membro (revisão do marco 2).
 */
@Repository
public class InvitationEmailLock {

	private static final ResultSetExtractor<Void> IGNORE_RESULT = (ResultSet resultSet) -> null;

	private final JdbcTemplate jdbcTemplate;

	InvitationEmailLock(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Espera até ser a única transação trabalhando com convites deste e-mail. Precisa rodar dentro da
	 * transação do chamador e antes de qualquer lock de linha, para que a ordem de locks seja sempre a mesma.
	 *
	 * <p>Exemplo: {@code invitationEmailLock.lockFor(email);}
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void lockFor(AccountEmail email) {
		// Colisão de hash só faz dois e-mails esperarem um pelo outro; nunca libera algo indevido.
		jdbcTemplate.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", IGNORE_RESULT,
				"invitation-email:" + email.value());
	}

}
