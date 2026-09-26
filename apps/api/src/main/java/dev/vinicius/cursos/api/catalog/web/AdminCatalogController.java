package dev.vinicius.cursos.api.catalog.web;

import dev.vinicius.cursos.api.catalog.service.CourseLifecycleService;
import dev.vinicius.cursos.api.catalog.service.CourseModuleOrganizer;
import dev.vinicius.cursos.api.catalog.service.LessonOrganizer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Organização do catálogo pelo ADMIN. Só traduz HTTP para os serviços do marco 1; as regras (títulos,
 * posições, publicação) continuam neles e as falhas viram 400/404/409 no {@code ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api/admin")
class AdminCatalogController {

	private final CourseLifecycleService lifecycleService;

	private final CourseModuleOrganizer moduleOrganizer;

	private final LessonOrganizer lessonOrganizer;

	AdminCatalogController(CourseLifecycleService lifecycleService, CourseModuleOrganizer moduleOrganizer,
			LessonOrganizer lessonOrganizer) {
		this.lifecycleService = lifecycleService;
		this.moduleOrganizer = moduleOrganizer;
		this.lessonOrganizer = lessonOrganizer;
	}

	/** Exemplo: {@code POST /api/admin/courses {"title":"Java","description":null} -> 201 {"id":"..."}}. */
	@PostMapping("/courses")
	@ResponseStatus(HttpStatus.CREATED)
	CreatedResource createCourse(@RequestBody CreateCourseRequest request) {
		return new CreatedResource(lifecycleService.createDraft(request.title(), request.description()));
	}

	/** Exemplo: {@code POST /api/admin/courses/{id}/publish -> 204}; incompleto responde 409. */
	@PostMapping("/courses/{courseId}/publish")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void publishCourse(@PathVariable UUID courseId) {
		lifecycleService.publish(courseId);
	}

	/** Exemplo: {@code POST /api/admin/courses/{id}/modules {"title":"Intro"} -> 201 {"id":"..."}}. */
	@PostMapping("/courses/{courseId}/modules")
	@ResponseStatus(HttpStatus.CREATED)
	CreatedResource appendModule(@PathVariable UUID courseId, @RequestBody TitleRequest request) {
		return new CreatedResource(moduleOrganizer.appendModule(courseId, request.title()));
	}

	/** Exemplo: {@code PUT /api/admin/courses/{id}/modules/order {"ids":[b,a]} -> 204}. */
	@PutMapping("/courses/{courseId}/modules/order")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void reorderModules(@PathVariable UUID courseId, @Valid @RequestBody ReorderRequest request) {
		moduleOrganizer.reorderModules(courseId, request.ids());
	}

	/** Exemplo: {@code POST /api/admin/modules/{id}/lessons {"title":"Aula 1"} -> 201 {"id":"..."}}. */
	@PostMapping("/modules/{moduleId}/lessons")
	@ResponseStatus(HttpStatus.CREATED)
	CreatedResource appendLesson(@PathVariable UUID moduleId, @RequestBody TitleRequest request) {
		return new CreatedResource(lessonOrganizer.appendLesson(moduleId, request.title()));
	}

	/** Exemplo: {@code PUT /api/admin/modules/{id}/lessons/order {"ids":[c,a,b]} -> 204}. */
	@PutMapping("/modules/{moduleId}/lessons/order")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void reorderLessons(@PathVariable UUID moduleId, @Valid @RequestBody ReorderRequest request) {
		lessonOrganizer.reorderLessons(moduleId, request.ids());
	}

	/** Corpo de criação de curso; a validação do título fica em {@code CatalogTitle}. */
	record CreateCourseRequest(String title, String description) {}

	/** Corpo com título de módulo ou aula. */
	record TitleRequest(String title) {}

	/** Lista completa de IDs filhos na nova ordem. */
	record ReorderRequest(@NotNull List<UUID> ids) {}

	/** ID do item criado. */
	record CreatedResource(UUID id) {}

}
