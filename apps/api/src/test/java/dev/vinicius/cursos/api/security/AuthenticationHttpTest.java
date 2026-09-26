package dev.vinicius.cursos.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.vinicius.cursos.api.account.domain.UserAccount;
import dev.vinicius.cursos.api.support.AccessIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Login, logout, CSRF e negação por padrão pela cadeia real do Spring Security. */
class AuthenticationHttpTest extends AccessIntegrationTest {

	@Autowired
	private Environment environment;

	@Test
	void csrfEndpointIsPublicAndTellsWhereToSendTheToken() throws Exception {
		mockMvc.perform(get("/api/auth/csrf"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
			.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void loginWithoutCsrfTokenIsForbidden() throws Exception {
		createMember("ana@x.com");

		mockMvc.perform(formLogin("ana@x.com", MEMBER_PASSWORD)).andExpect(status().isForbidden());
	}

	@Test
	void loginWithFetchedCsrfTokenCreatesSessionWithNewIdAndReturnsUser() throws Exception {
		UserAccount member = createMember("ana@x.com");
		MockHttpSession session = new MockHttpSession();
		String csrfToken = fetchCsrfToken(session);
		String anonymousSessionId = session.getId();

		MvcResult login = mockMvc
			.perform(formLogin(" ANA@x.com", MEMBER_PASSWORD).session(session).header("X-CSRF-TOKEN", csrfToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(member.getId().toString()))
			.andExpect(jsonPath("$.email").value("ana@x.com"))
			.andExpect(jsonPath("$.role").value("MEMBER"))
			.andReturn();

		assertThat(session.getId()).isNotEqualTo(anonymousSessionId);
		assertThat(login.getResponse().getContentAsString()).doesNotContain("password", "pbkdf2");
		mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
	}

	@Test
	void csrfTokenFromBeforeLoginNoLongerWorksAfterLogin() throws Exception {
		createMember("ana@x.com");
		MockHttpSession session = new MockHttpSession();
		String preLoginToken = fetchCsrfToken(session);
		mockMvc.perform(formLogin("ana@x.com", MEMBER_PASSWORD).session(session).header("X-CSRF-TOKEN", preLoginToken))
			.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN", preLoginToken))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN", fetchCsrfToken(session)))
			.andExpect(status().isNoContent());
	}

	@Test
	void anonymousCsrfSessionIsShortLivedAndLoginRestoresNormalTimeout() throws Exception {
		createMember("ana@x.com");
		MockHttpSession session = new MockHttpSession();
		String csrfToken = fetchCsrfToken(session);
		assertThat(session.getMaxInactiveInterval()).isEqualTo(15 * 60);

		mockMvc.perform(formLogin("ana@x.com", MEMBER_PASSWORD).session(session).header("X-CSRF-TOKEN", csrfToken))
			.andExpect(status().isOk());

		assertThat(session.getMaxInactiveInterval()).isEqualTo(8 * 60 * 60);
	}

	@Test
	void sessionStoresPrincipalWithoutPasswordHash() throws Exception {
		createMember("ana@x.com");
		MockHttpSession session = loginSession("ana@x.com", MEMBER_PASSWORD);

		SecurityContext storedContext = (SecurityContext) session
			.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

		LibraryUserPrincipal storedPrincipal = (LibraryUserPrincipal) storedContext.getAuthentication().getPrincipal();
		assertThat(storedPrincipal.getPassword()).isNull();
		assertThat(storedContext.getAuthentication().getCredentials()).isNull();
	}

	@Test
	void wrongPasswordAndUnknownEmailGetTheSameGenericAnswer() throws Exception {
		createMember("ana@x.com");

		String wrongPassword = mockMvc.perform(formLogin("ana@x.com", "senha errada mas longa").with(csrf()))
			.andExpect(status().isUnauthorized())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String unknownEmail = mockMvc.perform(formLogin("ninguem@x.com", "senha errada mas longa").with(csrf()))
			.andExpect(status().isUnauthorized())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(wrongPassword).isEqualTo(unknownEmail).contains("invalid email or password");
	}

	@Test
	void meRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void logoutInvalidatesTheSession() throws Exception {
		createMember("ana@x.com");
		MockHttpSession session = loginSession("ana@x.com", MEMBER_PASSWORD);

		mockMvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN", fetchCsrfToken(session)))
			.andExpect(status().isNoContent());

		assertThat(session.isInvalid()).isTrue();
	}

	@Test
	void logoutWithoutCsrfTokenIsForbiddenAndKeepsSession() throws Exception {
		createMember("ana@x.com");
		MockHttpSession session = loginSession("ana@x.com", MEMBER_PASSWORD);

		mockMvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden());

		mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
	}

	@Test
	void anonymousLogoutIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/auth/logout").with(csrf())).andExpect(status().isUnauthorized());
	}

	@Test
	void thereIsNoPublicSignupRoute() throws Exception {
		mockMvc
			.perform(post("/api/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ana@x.com\",\"password\":\"senha comprida qualquer\"}"))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/users").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isUnauthorized());
		assertThat(accountRepository.count()).isZero();
	}

	@Test
	void undeclaredRoutesAreDeniedEvenWhenAuthenticated() throws Exception {
		UserAccount member = createMember("ana@x.com");

		mockMvc.perform(get("/api/undeclared")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/undeclared").with(signedInAs(member))).andExpect(status().isForbidden());
		mockMvc.perform(get("/actuator/env").with(signedInAs(member))).andExpect(status().isForbidden());
	}

	@Test
	void basicHealthIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void responsesAskBrowsersNotToSendReferrer() throws Exception {
		mockMvc.perform(get("/api/auth/csrf")).andExpect(header().string("Referrer-Policy", "no-referrer"));
	}

	@Test
	void sessionCookieIsHttpOnlySecureAndLaxOutsideLocalProfile() {
		assertThat(environment.getProperty("server.servlet.session.cookie.http-only")).isEqualTo("true");
		assertThat(environment.getProperty("server.servlet.session.cookie.secure")).isEqualTo("true");
		assertThat(environment.getProperty("server.servlet.session.cookie.same-site")).isEqualTo("lax");
		assertThat(environment.getProperty("server.servlet.session.tracking-modes")).isEqualTo("cookie");
	}

	private static MockHttpServletRequestBuilder formLogin(String email, String password) {
		return post("/api/auth/login").contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("email", email)
			.param("password", password);
	}

}
