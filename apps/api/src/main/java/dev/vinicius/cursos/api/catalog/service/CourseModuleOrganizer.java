package dev.vinicius.cursos.api.catalog.service;

import dev.vinicius.cursos.api.catalog.domain.Course;
import dev.vinicius.cursos.api.catalog.domain.CourseModule;
import dev.vinicius.cursos.api.catalog.domain.CourseStatus;
import dev.vinicius.cursos.api.catalog.repository.CourseModuleRepository;
import dev.vinicius.cursos.api.catalog.repository.CourseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Acrescenta e reordena módulos dentro de um curso. */
@Service
public class CourseModuleOrganizer {

	private final CourseRepository courseRepository;

	private final CourseModuleRepository moduleRepository;

	private final ManualPositionReorderer positionReorderer;

	CourseModuleOrganizer(CourseRepository courseRepository, CourseModuleRepository moduleRepository,
			ManualPositionReorderer positionReorderer) {
		this.courseRepository = courseRepository;
		this.moduleRepository = moduleRepository;
		this.positionReorderer = positionReorderer;
	}

	/**
	 * Acrescenta um módulo no final de um curso em rascunho: posição 1 no primeiro, depois
	 * {@code max + 1}. Cursos publicados são rejeitados porque o módulo novo nasce sem aulas.
	 *
	 * <p>Exemplo: {@code UUID moduleId = organizer.appendModule(courseId, "Introdução");}
	 */
	@Transactional
	public UUID appendModule(UUID courseId, String title) {
		Course course = courseRepository.findById(courseId)
			.orElseThrow(() -> new CatalogItemNotFoundException("course", courseId));
		requireDraftForNewModule(course);
		int nextPosition = moduleRepository.findLastPositionInCourse(courseId) + 1;
		return moduleRepository.save(new CourseModule(course, title, nextPosition)).getId();
	}

	// Um módulo novo não tem aulas; em curso publicado isso quebraria a completude exigida em publish.
	// Não há despublicação automática: o estado só muda por transição explícita.
	private static void requireDraftForNewModule(Course course) {
		if (course.getStatus() != CourseStatus.DRAFT) {
			throw new PublishedCourseModificationException(
					"cannot append module to course %s: expected status DRAFT, got %s".formatted(course.getId(),
							course.getStatus()));
		}
	}

	/**
	 * Reordena todos os módulos do curso conforme a lista completa de IDs, deixando posições 1..N.
	 *
	 * <p>Exemplo: {@code organizer.reorderModules(courseId, List.of(secondId, firstId));}
	 */
	@Transactional
	public void reorderModules(UUID courseId, List<UUID> orderedModuleIds) {
		if (!courseRepository.existsById(courseId)) {
			throw new CatalogItemNotFoundException("course", courseId);
		}
		List<CourseModule> modules = moduleRepository.findByCourseIdOrderByPositionAsc(courseId);
		positionReorderer.reorder("course", courseId, modules, orderedModuleIds);
	}

}
