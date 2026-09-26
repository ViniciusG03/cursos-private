package dev.vinicius.cursos.api.catalog.service;

import dev.vinicius.cursos.api.catalog.domain.Course;
import dev.vinicius.cursos.api.catalog.domain.CourseStatus;
import dev.vinicius.cursos.api.catalog.repository.CourseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leitura do catálogo com a regra de visibilidade: membros só enxergam cursos publicados, inclusive
 * no acesso direto por UUID, em que um rascunho responde como inexistente.
 */
@Service
public class CourseBrowser {

	private final CourseRepository courseRepository;

	private final CourseOutlineReader outlineReader;

	CourseBrowser(CourseRepository courseRepository, CourseOutlineReader outlineReader) {
		this.courseRepository = courseRepository;
		this.outlineReader = outlineReader;
	}

	/**
	 * Lista os cursos visíveis por título.
	 *
	 * <p>Exemplo: {@code List<CourseSummary> courses = browser.listCourses(CourseVisibility.PUBLISHED_ONLY);}
	 */
	@Transactional(readOnly = true)
	public List<CourseSummary> listCourses(CourseVisibility visibility) {
		List<Course> courses = visibility == CourseVisibility.INCLUDING_DRAFTS ? courseRepository.findAllByOrderByTitleAsc()
				: courseRepository.findByStatusOrderByTitleAsc(CourseStatus.PUBLISHED);
		return courses.stream()
			.map(course -> new CourseSummary(course.getId(), course.getTitle(), course.getDescription(),
					course.getStatus()))
			.toList();
	}

	/**
	 * Curso completo e ordenado, ou {@link CatalogItemNotFoundException} se ele não existe ou não é
	 * visível (as duas situações respondem igual, para não confirmar a existência de rascunhos).
	 *
	 * <p>Exemplo: {@code CourseOutline outline = browser.readCourse(courseId, CourseVisibility.PUBLISHED_ONLY);}
	 */
	@Transactional(readOnly = true)
	public CourseOutline readCourse(UUID courseId, CourseVisibility visibility) {
		CourseOutline outline = outlineReader.readOutline(courseId);
		if (visibility == CourseVisibility.PUBLISHED_ONLY && outline.status() != CourseStatus.PUBLISHED) {
			throw new CatalogItemNotFoundException("course", courseId);
		}
		return outline;
	}

}
