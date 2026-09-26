package dev.vinicius.cursos.api.catalog.service;

import dev.vinicius.cursos.api.catalog.domain.Course;
import dev.vinicius.cursos.api.catalog.domain.CourseModule;
import dev.vinicius.cursos.api.catalog.repository.CourseModuleRepository;
import dev.vinicius.cursos.api.catalog.repository.CourseRepository;
import dev.vinicius.cursos.api.catalog.repository.LessonRepository;
import dev.vinicius.cursos.api.catalog.service.CourseOutline.LessonOutline;
import dev.vinicius.cursos.api.catalog.service.CourseOutline.ModuleOutline;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lê a hierarquia curso → módulos → aulas sempre por {@code position}, nunca por ID ou título. */
@Service
public class CourseOutlineReader {

	private final CourseRepository courseRepository;

	private final CourseModuleRepository moduleRepository;

	private final LessonRepository lessonRepository;

	CourseOutlineReader(CourseRepository courseRepository, CourseModuleRepository moduleRepository,
			LessonRepository lessonRepository) {
		this.courseRepository = courseRepository;
		this.moduleRepository = moduleRepository;
		this.lessonRepository = lessonRepository;
	}

	/**
	 * Monta o curso com módulos e aulas em ordem manual. Rascunhos podem vir sem módulos ou com
	 * módulos sem aulas.
	 *
	 * <p>Exemplo: {@code CourseOutline outline = reader.readOutline(courseId);}
	 */
	@Transactional(readOnly = true)
	public CourseOutline readOutline(UUID courseId) {
		Course course = courseRepository.findById(courseId)
			.orElseThrow(() -> new CatalogItemNotFoundException("course", courseId));
		List<ModuleOutline> modules = moduleRepository.findByCourseIdOrderByPositionAsc(courseId)
			.stream()
			.map(this::toModuleOutline)
			.toList();
		return new CourseOutline(course.getId(), course.getTitle(), course.getDescription(), course.getStatus(),
				modules);
	}

	private ModuleOutline toModuleOutline(CourseModule module) {
		List<LessonOutline> lessons = lessonRepository.findByModuleIdOrderByPositionAsc(module.getId())
			.stream()
			.map(lesson -> new LessonOutline(lesson.getId(), lesson.getTitle(), lesson.getPosition()))
			.toList();
		return new ModuleOutline(module.getId(), module.getTitle(), module.getPosition(), lessons);
	}

}
