package dev.vinicius.cursos.api.catalog.service;

import dev.vinicius.cursos.api.catalog.domain.Course;
import dev.vinicius.cursos.api.catalog.repository.CourseModuleRepository;
import dev.vinicius.cursos.api.catalog.repository.CourseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Criação de rascunhos e transição explícita para publicado. */
@Service
public class CourseLifecycleService {

	private final CourseRepository courseRepository;

	private final CourseModuleRepository moduleRepository;

	CourseLifecycleService(CourseRepository courseRepository, CourseModuleRepository moduleRepository) {
		this.courseRepository = courseRepository;
		this.moduleRepository = moduleRepository;
	}

	/**
	 * Cria um curso sempre em {@code DRAFT}; a descrição é opcional.
	 *
	 * <p>Exemplo: {@code UUID courseId = lifecycle.createDraft("Java moderno", null);}
	 */
	@Transactional
	public UUID createDraft(String title, String description) {
		return courseRepository.save(Course.draft(title, description)).getId();
	}

	/**
	 * Publica o curso se ele estiver estruturalmente completo: ao menos um módulo e ao menos uma
	 * aula em cada módulo. A exigência de vídeo pronto entra em um marco futuro.
	 *
	 * <p>Exemplo: {@code lifecycle.publish(courseId);}
	 */
	@Transactional
	public void publish(UUID courseId) {
		Course course = courseRepository.findById(courseId)
			.orElseThrow(() -> new CatalogItemNotFoundException("course", courseId));
		requireStructurallyComplete(courseId);
		course.markPublished();
	}

	private void requireStructurallyComplete(UUID courseId) {
		if (moduleRepository.countByCourseId(courseId) == 0) {
			throw new CourseNotPublishableException(
					"course %s cannot be published: expected at least one module, got 0".formatted(courseId));
		}
		List<UUID> emptyModuleIds = moduleRepository.findIdsWithoutLessonsInCourse(courseId);
		if (!emptyModuleIds.isEmpty()) {
			throw new CourseNotPublishableException(
					"course %s cannot be published: expected at least one lesson per module, modules without lessons: %s"
						.formatted(courseId, emptyModuleIds));
		}
	}

}
