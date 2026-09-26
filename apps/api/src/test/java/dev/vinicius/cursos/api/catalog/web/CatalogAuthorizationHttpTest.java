package dev.vinicius.cursos.api.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.vinicius.cursos.api.account.domain.UserAccount;
import dev.vinicius.cursos.api.support.AccessIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Somente o ADMIN altera o catálogo; MEMBER lê apenas cursos publicados, inclusive por UUID. */
class CatalogAuthorizationHttpTest extends AccessIntegrationTest {

	private UserAccount admin;

	private UserAccount member;

	@BeforeEach
	void createAccounts() {
		admin = createAdmin();
		member = createMember("ana@x.com");
	}

	@Test
	void anonymousCannotReadCatalog() throws Exception {
		UUID publishedId = createPublishedCourse("Publicado");

		mockMvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/courses/" + publishedId)).andExpect(status().isUnauthorized());
	}

	@Test
	void anonymousAdminOperationIsUnauthorizedAndWithoutCsrfIsForbidden() throws Exception {
		mockMvc.perform(jsonPost("/api/admin/courses", "{\"title\":\"Java\"}").with(csrf()))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(jsonPost("/api/admin/courses", "{\"title\":\"Java\"}")).andExpect(status().isForbidden());
		assertThat(countCourses()).isZero();
	}

	@Test
	void memberCannotChangeCatalog() throws Exception {
		UUID draftId = createDraftWithOneLesson("Rascunho");
		UUID moduleId = firstModuleId(draftId);

		expectForbiddenForMember(jsonPost("/api/admin/courses", "{\"title\":\"Java\"}"));
		expectForbiddenForMember(post("/api/admin/courses/" + draftId + "/publish"));
		expectForbiddenForMember(jsonPost("/api/admin/courses/" + draftId + "/modules", "{\"title\":\"M\"}"));
		expectForbiddenForMember(jsonPost("/api/admin/modules/" + moduleId + "/lessons", "{\"title\":\"A\"}"));
		expectForbiddenForMember(jsonPut("/api/admin/courses/" + draftId + "/modules/order", "{\"ids\":[]}"));
		expectForbiddenForMember(jsonPut("/api/admin/modules/" + moduleId + "/lessons/order", "{\"ids\":[]}"));
		expectForbiddenForMember(jsonPost("/api/admin/invitations", "{\"email\":\"bia@x.com\"}"));

		assertThat(countCourses()).isEqualTo(1);
		assertThat(readCourseStatus(draftId)).isEqualTo("DRAFT");
		assertThat(mailbox.deliveredMails()).isEmpty();
	}

	@Test
	void adminOrganizesAndPublishesCourse() throws Exception {
		UUID courseId = createdId(jsonPost("/api/admin/courses", "{\"title\":\"Java\",\"description\":\"Do zero\"}"));
		UUID firstModuleId = createdId(jsonPost("/api/admin/courses/" + courseId + "/modules", "{\"title\":\"Um\"}"));
		UUID secondModuleId = createdId(jsonPost("/api/admin/courses/" + courseId + "/modules", "{\"title\":\"Dois\"}"));
		createdId(jsonPost("/api/admin/modules/" + firstModuleId + "/lessons", "{\"title\":\"Aula 1.1\"}"));
		createdId(jsonPost("/api/admin/modules/" + secondModuleId + "/lessons", "{\"title\":\"Aula 2.1\"}"));

		mockMvc.perform(jsonPut("/api/admin/courses/" + courseId + "/modules/order",
				"{\"ids\":[\"" + secondModuleId + "\",\"" + firstModuleId + "\"]}").with(signedInAs(admin)).with(csrf()))
			.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/admin/courses/" + courseId + "/publish").with(signedInAs(admin)).with(csrf()))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/courses/" + courseId).with(signedInAs(member)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PUBLISHED"))
			.andExpect(jsonPath("$.modules[0].title").value("Dois"))
			.andExpect(jsonPath("$.modules[1].lessons[0].title").value("Aula 1.1"));
	}

	@Test
	void adminWriteWithoutCsrfIsForbidden() throws Exception {
		mockMvc.perform(jsonPost("/api/admin/courses", "{\"title\":\"Java\"}").with(signedInAs(admin)))
			.andExpect(status().isForbidden());
	}

	@Test
	void memberListsOnlyPublishedCoursesAndAdminSeesDrafts() throws Exception {
		createPublishedCourse("Publicado");
		createDraftWithOneLesson("Rascunho");

		mockMvc.perform(get("/api/courses").with(signedInAs(member)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].title", contains("Publicado")));
		mockMvc.perform(get("/api/courses").with(signedInAs(admin)))
			.andExpect(jsonPath("$[*].title", contains("Publicado", "Rascunho")));
	}

	@Test
	void directUuidAccessToDraftLooksLikeMissingCourseForMember() throws Exception {
		UUID draftId = createDraftWithOneLesson("Rascunho");

		String draftBody = mockMvc.perform(get("/api/courses/" + draftId).with(signedInAs(member)))
			.andExpect(status().isNotFound())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String missingBody = mockMvc.perform(get("/api/courses/" + UUID.randomUUID()).with(signedInAs(member)))
			.andExpect(status().isNotFound())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(draftBody).doesNotContain("Rascunho");
		assertThat(missingBody).contains("course not found");
		mockMvc.perform(get("/api/courses/" + draftId).with(signedInAs(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("DRAFT"));
	}

	@Test
	void catalogRuleViolationsMapToClientErrors() throws Exception {
		UUID emptyCourseId = createdId(jsonPost("/api/admin/courses", "{\"title\":\"Vazio\"}"));

		expectAdminStatus(post("/api/admin/courses/" + emptyCourseId + "/publish"), 409);
		expectAdminStatus(jsonPost("/api/admin/courses", "{\"title\":\"   \"}"), 400);
		expectAdminStatus(post("/api/admin/courses/" + UUID.randomUUID() + "/publish"), 404);
		expectAdminStatus(post("/api/admin/courses/nao-e-uuid/publish"), 400);
		expectAdminStatus(jsonPut("/api/admin/courses/" + emptyCourseId + "/modules/order",
				"{\"ids\":[\"" + UUID.randomUUID() + "\"]}"), 400);
	}

	private void expectForbiddenForMember(MockHttpServletRequestBuilder request) throws Exception {
		mockMvc.perform(request.with(signedInAs(member)).with(csrf())).andExpect(status().isForbidden());
	}

	private void expectAdminStatus(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
		mockMvc.perform(request.with(signedInAs(admin)).with(csrf())).andExpect(status().is(expectedStatus));
	}

	private UUID createPublishedCourse(String title) throws Exception {
		UUID courseId = createDraftWithOneLesson(title);
		mockMvc.perform(post("/api/admin/courses/" + courseId + "/publish").with(signedInAs(admin)).with(csrf()))
			.andExpect(status().isNoContent());
		return courseId;
	}

	private UUID createDraftWithOneLesson(String title) throws Exception {
		UUID courseId = createdId(jsonPost("/api/admin/courses", "{\"title\":\"" + title + "\"}"));
		UUID moduleId = createdId(jsonPost("/api/admin/courses/" + courseId + "/modules", "{\"title\":\"Módulo\"}"));
		createdId(jsonPost("/api/admin/modules/" + moduleId + "/lessons", "{\"title\":\"Aula\"}"));
		return courseId;
	}

	private UUID createdId(MockHttpServletRequestBuilder request) throws Exception {
		RequestPostProcessor asAdmin = signedInAs(admin);
		String body = mockMvc.perform(request.with(asAdmin).with(csrf()))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return UUID.fromString(JsonPath.read(body, "$.id"));
	}

	private UUID firstModuleId(UUID courseId) {
		return jdbcTemplate.queryForObject("SELECT id FROM modules WHERE course_id = ? ORDER BY position LIMIT 1",
				UUID.class, courseId);
	}

	private int countCourses() {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM courses", Integer.class);
	}

	private String readCourseStatus(UUID courseId) {
		return jdbcTemplate.queryForObject("SELECT status FROM courses WHERE id = ?", String.class, courseId);
	}

	private static MockHttpServletRequestBuilder jsonPost(String path, String json) {
		return post(path).contentType(MediaType.APPLICATION_JSON).content(json);
	}

	private static MockHttpServletRequestBuilder jsonPut(String path, String json) {
		return put(path).contentType(MediaType.APPLICATION_JSON).content(json);
	}

}
