package dev.vinicius.cursos.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.passwordreset.service.PasswordResetConfirmation;
import dev.vinicius.cursos.api.passwordreset.service.PasswordResetRequestService;
import dev.vinicius.cursos.api.support.AccessIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

/**
 * Sessões abertas antes de uma troca de senha deixam de valer, e o papel é relido do banco a cada
 * requisição. A revogação compara {@code credential_version}, sem depender de timestamps.
 */
class SessionRevocationHttpTest extends AccessIntegrationTest {

	private static final String NEW_PASSWORD = "senha nova depois do susto";

	@Autowired
	private PasswordResetRequestService resetRequestService;

	@Autowired
	private PasswordResetConfirmation resetConfirmation;

	@Test
	void sessionOpenedBeforePasswordResetIsRejectedAndInvalidated() throws Exception {
		createMember("ana@x.com");
		MockHttpSession oldSession = loginSession("ana@x.com", MEMBER_PASSWORD);
		mockMvc.perform(get("/api/auth/me").session(oldSession)).andExpect(status().isOk());

		resetPassword("ana@x.com");

		mockMvc.perform(get("/api/auth/me").session(oldSession)).andExpect(status().isUnauthorized());
		assertThat(oldSession.isInvalid()).isTrue();
	}

	@Test
	void everySessionOfTheAccountIsRevokedButNewLoginWorks() throws Exception {
		createMember("ana@x.com");
		MockHttpSession laptop = loginSession("ana@x.com", MEMBER_PASSWORD);
		MockHttpSession phone = loginSession("ana@x.com", MEMBER_PASSWORD);

		resetPassword("ana@x.com");

		mockMvc.perform(get("/api/courses").session(laptop)).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/courses").session(phone)).andExpect(status().isUnauthorized());
		MockHttpSession fresh = loginSession("ana@x.com", NEW_PASSWORD);
		mockMvc.perform(get("/api/auth/me").session(fresh)).andExpect(status().isOk());
	}

	@Test
	void otherAccountsKeepTheirSessions() throws Exception {
		createMember("ana@x.com");
		createMember("bia@x.com");
		MockHttpSession biaSession = loginSession("bia@x.com", MEMBER_PASSWORD);

		resetPassword("ana@x.com");

		mockMvc.perform(get("/api/auth/me").session(biaSession)).andExpect(status().isOk());
	}

	@Test
	void roleIsReadFromDatabaseOnEveryRequest() throws Exception {
		createAdmin();
		MockHttpSession adminSession = loginSession(ADMIN_EMAIL, ADMIN_PASSWORD);
		jdbcTemplate.update("UPDATE users SET role = 'MEMBER' WHERE email = ?", ADMIN_EMAIL);

		mockMvc
			.perform(post("/api/admin/courses").session(adminSession)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Java\"}"))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/auth/me").session(adminSession)).andExpect(jsonPath("$.role").value("MEMBER"));
	}

	@Test
	void sessionOfRemovedAccountIsRejected() throws Exception {
		createMember("ana@x.com");
		MockHttpSession session = loginSession("ana@x.com", MEMBER_PASSWORD);
		jdbcTemplate.update("DELETE FROM users WHERE email = 'ana@x.com'");

		mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
	}

	private void resetPassword(String email) {
		resetRequestService.requestReset(AccountEmail.parse(email));
		resetConfirmation.confirm(awaitResetLinkTo(email, 1), NEW_PASSWORD);
	}

}
