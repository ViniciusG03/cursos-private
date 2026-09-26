package dev.vinicius.cursos.api.catalog.service;

import dev.vinicius.cursos.api.catalog.domain.CourseModule;
import dev.vinicius.cursos.api.catalog.domain.Lesson;
import dev.vinicius.cursos.api.catalog.repository.CourseModuleRepository;
import dev.vinicius.cursos.api.catalog.repository.LessonRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Acrescenta e reordena aulas dentro de um módulo. A posição pertence ao módulo, não ao curso. */
@Service
public class LessonOrganizer {

	private final CourseModuleRepository moduleRepository;

	private final LessonRepository lessonRepository;

	private final ManualPositionReorderer positionReorderer;

	LessonOrganizer(CourseModuleRepository moduleRepository, LessonRepository lessonRepository,
			ManualPositionReorderer positionReorderer) {
		this.moduleRepository = moduleRepository;
		this.lessonRepository = lessonRepository;
		this.positionReorderer = positionReorderer;
	}

	/**
	 * Acrescenta uma aula no final do módulo: posição 1 na primeira, depois {@code max + 1}.
	 * Não publica o curso automaticamente.
	 *
	 * <p>Exemplo: {@code UUID lessonId = organizer.appendLesson(moduleId, "Instalando o JDK");}
	 */
	@Transactional
	public UUID appendLesson(UUID moduleId, String title) {
		CourseModule module = moduleRepository.findById(moduleId)
			.orElseThrow(() -> new CatalogItemNotFoundException("module", moduleId));
		int nextPosition = lessonRepository.findLastPositionInModule(moduleId) + 1;
		return lessonRepository.save(new Lesson(module, title, nextPosition)).getId();
	}

	/**
	 * Reordena todas as aulas do módulo conforme a lista completa de IDs, deixando posições 1..N.
	 *
	 * <p>Exemplo: {@code organizer.reorderLessons(moduleId, List.of(thirdId, firstId, secondId));}
	 */
	@Transactional
	public void reorderLessons(UUID moduleId, List<UUID> orderedLessonIds) {
		if (!moduleRepository.existsById(moduleId)) {
			throw new CatalogItemNotFoundException("module", moduleId);
		}
		List<Lesson> lessons = lessonRepository.findByModuleIdOrderByPositionAsc(moduleId);
		positionReorderer.reorder("module", moduleId, lessons, orderedLessonIds);
	}

}
