package dev.vinicius.cursos.api.catalog.web;

import dev.vinicius.cursos.api.account.domain.UserRole;
import dev.vinicius.cursos.api.catalog.service.CourseBrowser;
import dev.vinicius.cursos.api.catalog.service.CourseOutline;
import dev.vinicius.cursos.api.catalog.service.CourseSummary;
import dev.vinicius.cursos.api.catalog.service.CourseVisibility;
import dev.vinicius.cursos.api.security.LibraryUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Leitura do catálogo por qualquer usuário autenticado; a visibilidade vem do papel atual no banco. */
@RestController
@RequestMapping("/api/courses")
class CourseCatalogController {

	private final CourseBrowser courseBrowser;

	CourseCatalogController(CourseBrowser courseBrowser) {
		this.courseBrowser = courseBrowser;
	}

	/**
	 * Cursos visíveis: publicados para MEMBER, todos para ADMIN.
	 *
	 * <p>Exemplo: {@code GET /api/courses -> 200 [{"id":"...","status":"PUBLISHED",...}]}.
	 */
	@GetMapping
	List<CourseSummary> listCourses(@AuthenticationPrincipal LibraryUserPrincipal reader) {
		return courseBrowser.listCourses(visibilityFor(reader));
	}

	/**
	 * Curso com módulos e aulas em ordem; rascunho para MEMBER responde 404.
	 *
	 * <p>Exemplo: {@code GET /api/courses/{id} -> 200 {"modules":[{"lessons":[...]}]}}.
	 */
	@GetMapping("/{courseId}")
	CourseOutline readCourse(@PathVariable UUID courseId, @AuthenticationPrincipal LibraryUserPrincipal reader) {
		return courseBrowser.readCourse(courseId, visibilityFor(reader));
	}

	private static CourseVisibility visibilityFor(LibraryUserPrincipal reader) {
		return reader.role() == UserRole.ADMIN ? CourseVisibility.INCLUDING_DRAFTS : CourseVisibility.PUBLISHED_ONLY;
	}

}
