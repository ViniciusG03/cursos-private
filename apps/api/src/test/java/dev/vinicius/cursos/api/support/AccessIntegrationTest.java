package dev.vinicius.cursos.api.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.account.domain.UserAccount;
import dev.vinicius.cursos.api.account.repository.UserAccountRepository;
import dev.vinicius.cursos.api.account.service.PasswordHasher;
import dev.vinicius.cursos.api.security.LibraryUserPrincipal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Base dos testes de contas, convites, recuperação e autorização contra PostgreSQL real. Limpa todas as
 * tabelas, o relógio e a caixa de e-mails antes de cada teste; não é transacional pelo mesmo motivo de
 * {@code CatalogIntegrationTest}: cada serviço confirma a própria transação.
 */
@LibraryIntegrationTest
public abstract class AccessIntegrationTest {

	protected static final String ADMIN_EMAIL = "admin@biblioteca.test";

	protected static final String ADMIN_PASSWORD = "senha do administrador 2026";

	protected static final String MEMBER_PASSWORD = "senha comprida do membro";

	// PBKDF2 é lento de propósito; os testes reaproveitam o hash de cada senha repetida.
	private static final Map<String, String> HASH_BY_PASSWORD = new ConcurrentHashMap<>();

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected RecordingMailSender mailbox;

	@Autowired
	protected MutableTestClock clock;

	@Autowired
	protected UserAccountRepository accountRepository;

	@Autowired
	protected PasswordHasher passwordHasher;

	@BeforeEach
	protected void resetAccessState() {
		jdbcTemplate.execute("""
				TRUNCATE auth_attempt_windows, password_reset_requests, password_reset_tokens, invitations, users, lessons,
				modules, courses""");
		clock.reset();
		mailbox.reset();
	}

	protected UserAccount createAdmin() {
		return accountRepository.saveAndFlush(
				UserAccount.admin(AccountEmail.parse(ADMIN_EMAIL), cachedHash(ADMIN_PASSWORD), clock.instant()));
	}

	protected UserAccount createMember(String email) {
		return accountRepository.saveAndFlush(
				UserAccount.member(AccountEmail.parse(email), cachedHash(MEMBER_PASSWORD), clock.instant()));
	}

	/** Sessão autenticada sem passar pelo login, validada pelo filtro de credenciais contra o banco. */
	protected static RequestPostProcessor signedInAs(UserAccount account) {
		LibraryUserPrincipal principal = LibraryUserPrincipal.authenticated(account);
		return authentication(UsernamePasswordAuthenticationToken.authenticated(principal, null,
				principal.getAuthorities()));
	}

	/** Token CSRF obtido pela rota pública, como a SPA fará. */
	protected String fetchCsrfToken(MockHttpSession session) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
	}

	/** Login real (formulário + CSRF buscado na rota) que devolve a sessão autenticada. */
	protected MockHttpSession loginSession(String email, String password) throws Exception {
		MockHttpSession session = new MockHttpSession();
		mockMvc
			.perform(post("/api/auth/login").session(session)
				.header("X-CSRF-TOKEN", fetchCsrfToken(session))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("email", email)
				.param("password", password))
			.andExpect(status().isOk());
		return session;
	}

	/**
	 * Espera o {@code mailNumber}-ésimo link de recuperação ao destinatário e a confirmação da transação do
	 * worker. A fake registra o e-mail antes do commit; sem esperar o commit, o token ainda não estaria visível.
	 *
	 * <p>Exemplo: {@code String rawToken = awaitResetLinkTo("ana@x.com", 1);}
	 */
	protected String awaitResetLinkTo(String normalizedEmail, int mailNumber) {
		String rawToken = mailbox.awaitTokenSentTo(normalizedEmail, mailNumber);
		awaitDueResetRequestsHandled();
		return rawToken;
	}

	/**
	 * Espera (até 10 s) o worker real concluir ou reagendar todos os pedidos de recuperação vencidos.
	 *
	 * <p>Exemplo: {@code awaitDueResetRequestsHandled();}
	 */
	protected void awaitDueResetRequestsHandled() {
		java.time.Instant deadline = java.time.Instant.now().plusSeconds(10);
		while (countDueResetRequests() > 0) {
			if (java.time.Instant.now().isAfter(deadline)) {
				throw new AssertionError("expected the reset worker to handle due requests within 10s, got %d pending"
					.formatted(countDueResetRequests()));
			}
			pauseBriefly();
		}
	}

	private static void pauseBriefly() {
		try {
			Thread.sleep(20);
		}
		catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new AssertionError("interrupted while waiting for the reset worker", interrupted);
		}
	}

	/**
	 * Espera (até 10 s) alguma sessão do banco ficar parada esperando um lock. Garante que a ordem forçada
	 * de um teste concorrente aconteceu de fato, em vez de depender do agendador de threads.
	 *
	 * <p>Exemplo: {@code awaitSessionWaitingOnLock();}
	 */
	protected void awaitSessionWaitingOnLock() {
		java.time.Instant deadline = java.time.Instant.now().plusSeconds(10);
		while (countSessionsWaitingOnLock() == 0) {
			if (java.time.Instant.now().isAfter(deadline)) {
				throw new AssertionError("expected a database session waiting on a lock within 10s, got none");
			}
			pauseBriefly();
		}
	}

	private int countSessionsWaitingOnLock() {
		return jdbcTemplate.queryForObject("""
				SELECT count(*) FROM pg_stat_activity
				WHERE datname = current_database() AND wait_event_type = 'Lock'""", Integer.class);
	}

	private int countDueResetRequests() {
		return jdbcTemplate.queryForObject(
				"SELECT count(*) FROM password_reset_requests WHERE status = 'PENDING' AND next_attempt_at <= ?",
				Integer.class, java.sql.Timestamp.from(clock.instant()));
	}

	protected static String cachedHashFor(PasswordHasher hasher, String password) {
		return HASH_BY_PASSWORD.computeIfAbsent(password, hasher::hashAcceptable);
	}

	private String cachedHash(String password) {
		return cachedHashFor(passwordHasher, password);
	}

}
