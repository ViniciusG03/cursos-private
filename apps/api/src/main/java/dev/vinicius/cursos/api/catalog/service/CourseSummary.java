package dev.vinicius.cursos.api.catalog.service;

import dev.vinicius.cursos.api.catalog.domain.CourseStatus;
import java.util.UUID;

/**
 * Curso na listagem do catálogo, sem módulos.
 *
 * <p>Exemplo: {@code summary.status() == CourseStatus.PUBLISHED}.
 */
public record CourseSummary(UUID id, String title, String description, CourseStatus status) {}
