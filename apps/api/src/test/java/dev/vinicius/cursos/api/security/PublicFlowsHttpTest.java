package dev.vinicius.cursos.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.vinicius.cursos.api.account.domain.UserAccount;
import dev.vinicius.cursos.api.support.AccessIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Convite, recuperação e limites de tentativa pelas rotas públicas, com CSRF. */
class PublicFlowsHttpTest extends AccessIntegrationTest {

	private static final String CHOSEN_PASSWORD = "senha escolhida pela ana";

	private UserAccount admin;

	@BeforeEach
	void createAdminAccount() {
		admin = createAdmin();
	}

	@Test
	void invitedPersonDefinesPasswordAndSignsIn() throws Exception {
		MockHttpServletResponse issued = mockMvc
			.perform(jsonPost("/api/admin/invitations", "{\"email\":\"Ana@X.com\"}").with(signedInAs(admin)).with(csrf()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value("ana@x.com"))
			.andExpect(jsonPath("$.deliveryStatus").value("SENT"))
			.andReturn()
			.getResponse();
		String rawToken = mailbox.lastTokenSentTo("ana@x.com");
		assertThat(issued.getContentAsString()).doesNotContain(rawToken, "tokenHash", "token_hash");

		MockHttpSession session = new MockHttpSession();
		mockMvc
			.perform(acceptRequest(rawToken, CHOSEN_PASSWORD).session(session)
				.header("X-CSRF-TOKEN", fetchCsrfToken(session)))
			.andExpect(status().isNoContent());

		MockHttpSession memberSession = loginSession("ana@x.com", CHOSEN_PASSWORD);
		mockMvc.perform(get("/api/auth/me").session(memberSession)).andExpect(jsonPath("$.role").value("MEMBER"));
	}

	@Test
	void chosenPasswordAuthenticatesOnlyThatAccount() throws Exception {
		createMember("bia@x.com");
		acceptInvitation("ana@x.com", CHOSEN_PASSWORD);

		mockMvc.perform(formLogin("bia@x.com", CHOSEN_PASSWORD).with(csrf())).andExpect(status().isUnauthorized());
		mockMvc.perform(formLogin(ADMIN_EMAIL, CHOSEN_PASSWORD).with(csrf())).andExpect(status().isUnauthorized());
		mockMvc.perform(formLogin("ana@x.com", CHOSEN_PASSWORD).with(csrf())).andExpect(status().isOk());
	}

	@Test
	void acceptanceWithoutCsrfIsForbidden() throws Exception {
		mockMvc.perform(acceptRequest("qualquer", CHOSEN_PASSWORD)).andExpect(status().isForbidden());
	}

	@Test
	void malformedBodyOnPublicRouteIsBadRequestWithoutEchoingIt() throws Exception {
		String body = mockMvc
			.perform(jsonPost("/api/auth/invitations/accept", "{\"token\": \"abc\", \"password\": " + CHOSEN_PASSWORD)
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(body).doesNotContain(CHOSEN_PASSWORD, "abc");
	}

	@Test
	void missingFieldsAreReportedByNameOnly() throws Exception {
		mockMvc.perform(jsonPost("/api/auth/invitations/accept", "{\"password\":\"" + CHOSEN_PASSWORD + "\"}").with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("invalid request fields (values omitted): [token must not be blank]"));
	}

	@Test
	void invalidTokenIsRejectedWithoutEchoingIt() throws Exception {
		String body = mockMvc.perform(acceptRequest("token-inventado-123", CHOSEN_PASSWORD).with(csrf()))
			.andExpect(status().isBadRequest())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(body).contains("token is invalid or expired").doesNotContain("token-inventado-123");
	}

	@Test
	void weakPasswordOnAcceptanceIsRejectedWithoutEchoingIt() throws Exception {
		mockMvc.perform(jsonPost("/api/admin/invitations", "{\"email\":\"ana@x.com\"}").with(signedInAs(admin)).with(csrf()));

		String body = mockMvc.perform(acceptRequest(mailbox.lastTokenSentTo("ana@x.com"), "curtinha").with(csrf()))
			.andExpect(status().isBadRequest())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(body).contains("got 8").doesNotContain("curtinha");
	}

	@Test
	void resetRequestAnswersIdenticallyForExistingAndUnknownEmail() throws Exception {
		createMember("ana@x.com");

		MockHttpServletResponse existing = mockMvc
			.perform(jsonPost("/api/auth/password-resets/request", "{\"email\":\"ana@x.com\"}").with(csrf()))
			.andReturn()
			.getResponse();
		MockHttpServletResponse unknown = mockMvc
			.perform(jsonPost("/api/auth/password-resets/request", "{\"email\":\"ninguem@x.com\"}").with(csrf()))
			.andReturn()
			.getResponse();

		assertThat(existing.getStatus()).isEqualTo(unknown.getStatus()).isEqualTo(202);
		assertThat(existing.getContentAsString()).isEqualTo(unknown.getContentAsString()).isEmpty();
		awaitResetLinkTo("ana@x.com", 1);
		assertThat(mailbox.deliveredMails()).hasSize(1);
	}

	@Test
	void resetConfirmationSwapsPasswordWithoutCreatingSession() throws Exception {
		createMember("ana@x.com");
		mockMvc.perform(jsonPost("/api/auth/password-resets/request", "{\"email\":\"ana@x.com\"}").with(csrf()));
		String rawToken = awaitResetLinkTo("ana@x.com", 1);
		MockHttpSession session = new MockHttpSession();

		mockMvc
			.perform(jsonPost("/api/auth/password-resets/confirm",
					"{\"token\":\"" + rawToken + "\",\"password\":\"" + CHOSEN_PASSWORD + "\"}")
				.session(session)
				.header("X-CSRF-TOKEN", fetchCsrfToken(session)))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
		mockMvc.perform(formLogin("ana@x.com", MEMBER_PASSWORD).with(csrf())).andExpect(status().isUnauthorized());
		mockMvc.perform(formLogin("ana@x.com", CHOSEN_PASSWORD).with(csrf())).andExpect(status().isOk());
	}

	@Test
	void repeatedLoginAttemptsAreLimitedEvenWithTheRightPassword() throws Exception {
		createMember("ana@x.com");
		for (int attempt = 0; attempt < 5; attempt++) {
			mockMvc.perform(formLogin("ana@x.com", "senha errada mas longa").with(csrf()))
				.andExpect(status().isUnauthorized());
		}

		mockMvc.perform(formLogin("ana@x.com", MEMBER_PASSWORD).with(csrf()))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"));
	}

	@Test
	void repeatedResetRequestsAreLimited() throws Exception {
		for (int attempt = 0; attempt < 5; attempt++) {
			mockMvc.perform(jsonPost("/api/auth/password-resets/request", "{\"email\":\"ana@x.com\"}").with(csrf()))
				.andExpect(status().isAccepted());
		}

		mockMvc.perform(jsonPost("/api/auth/password-resets/request", "{\"email\":\"ana@x.com\"}").with(csrf()))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"));
	}

	@Test
	void repeatedTokenGuessesFromOneOriginAreLimited() throws Exception {
		for (int attempt = 0; attempt < 20; attempt++) {
			mockMvc.perform(acceptRequest("chute-" + attempt, CHOSEN_PASSWORD).with(csrf()))
				.andExpect(status().isBadRequest());
		}

		mockMvc.perform(acceptRequest("chute-final", CHOSEN_PASSWORD).with(csrf()))
			.andExpect(status().isTooManyRequests());
		mockMvc
			.perform(jsonPost("/api/auth/password-resets/confirm",
					"{\"token\":\"outro\",\"password\":\"" + CHOSEN_PASSWORD + "\"}")
				.with(csrf()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void repeatedResetConfirmationsFromOneOriginAreLimited() throws Exception {
		for (int attempt = 0; attempt < 20; attempt++) {
			mockMvc.perform(resetConfirmRequest("chute-" + attempt).with(csrf())).andExpect(status().isBadRequest());
		}

		mockMvc.perform(resetConfirmRequest("chute-final").with(csrf()))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"));
	}

	@Test
	void loginAttemptsFromAnotherOriginDoNotLockOutTheOwner() throws Exception {
		createMember("ana@x.com");
		for (int attempt = 0; attempt < 5; attempt++) {
			mockMvc.perform(formLogin("ana@x.com", "senha errada mas longa").with(csrf()).with(fromAddress("203.0.113.7")))
				.andExpect(status().isUnauthorized());
		}

		mockMvc.perform(formLogin("ana@x.com", MEMBER_PASSWORD).with(csrf())).andExpect(status().isOk());
	}

	private static RequestPostProcessor fromAddress(String remoteAddress) {
		return request -> {
			request.setRemoteAddr(remoteAddress);
			return request;
		};
	}

	private static MockHttpServletRequestBuilder resetConfirmRequest(String rawToken) {
		return jsonPost("/api/auth/password-resets/confirm",
				"{\"token\":\"" + rawToken + "\",\"password\":\"" + CHOSEN_PASSWORD + "\"}");
	}

	private void acceptInvitation(String email, String password) throws Exception {
		mockMvc.perform(jsonPost("/api/admin/invitations", "{\"email\":\"" + email + "\"}").with(signedInAs(admin)).with(csrf()))
			.andExpect(status().isCreated());
		mockMvc.perform(acceptRequest(mailbox.lastTokenSentTo(email), password).with(csrf()))
			.andExpect(status().isNoContent());
	}

	private static MockHttpServletRequestBuilder acceptRequest(String rawToken, String password) {
		return jsonPost("/api/auth/invitations/accept",
				"{\"token\":\"" + rawToken + "\",\"password\":\"" + password + "\"}");
	}

	private static MockHttpServletRequestBuilder jsonPost(String path, String json) {
		return post(path).contentType(MediaType.APPLICATION_JSON).content(json);
	}

	private static MockHttpServletRequestBuilder formLogin(String email, String password) {
		return post("/api/auth/login").contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("email", email)
			.param("password", password);
	}

}
