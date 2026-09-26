package dev.vinicius.cursos.api.catalog.service;

import dev.vinicius.cursos.api.catalog.domain.CourseStatus;
import java.util.List;
import java.util.UUID;

/**
 * Hierarquia de um curso já ordenada por posição. São records (e não entidades) para não expor
 * associações lazy fora da transação de leitura.
 *
 * <p>Exemplo: {@code outline.modules().getFirst().lessons()}.
 */
public record CourseOutline(UUID id, String title, String description, CourseStatus status,
		List<ModuleOutline> modules) {

	/** Módulo e suas aulas em ordem manual. Exemplo: {@code moduleOutline.position()}. */
	public record ModuleOutline(UUID id, String title, int position, List<LessonOutline> lessons) {}

	/** Aula em sua posição no módulo. Exemplo: {@code lessonOutline.title()}. */
	public record LessonOutline(UUID id, String title, int position) {}

}
