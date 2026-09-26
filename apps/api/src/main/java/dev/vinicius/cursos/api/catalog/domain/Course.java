package dev.vinicius.cursos.api.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Curso do catálogo, mapeado sobre {@code courses} (V1). Nasce sempre como rascunho. */
@Entity
@Table(name = "courses")
public class Course {

	public static final int TITLE_MAX_LENGTH = 250;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
	private String title;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	// O default 'DRAFT' do banco não se aplica a inserts do Hibernate, que enviam todas as colunas.
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private CourseStatus status;

	protected Course() {}

	private Course(String title, String description) {
		this.title = CatalogTitle.requireValid(title, TITLE_MAX_LENGTH, "course title");
		this.description = description;
		this.status = CourseStatus.DRAFT;
	}

	/**
	 * Cria um curso novo em {@link CourseStatus#DRAFT}; a descrição pode ser nula.
	 *
	 * <p>Exemplo: {@code Course.draft("Java moderno", null)}.
	 */
	public static Course draft(String title, String description) {
		return new Course(title, description);
	}

	/**
	 * Marca o curso como publicado. A completude estrutural é verificada pelo serviço antes.
	 *
	 * <p>Exemplo: {@code course.markPublished()}.
	 */
	public void markPublished() {
		this.status = CourseStatus.PUBLISHED;
	}

	public UUID getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public CourseStatus getStatus() {
		return status;
	}

}
