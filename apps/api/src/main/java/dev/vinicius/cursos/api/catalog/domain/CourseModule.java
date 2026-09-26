package dev.vinicius.cursos.api.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Módulo de um curso, mapeado sobre {@code modules} (V2). Chama-se {@code CourseModule} para não
 * colidir com {@link java.lang.Module}.
 */
@Entity
@Table(name = "modules")
public class CourseModule implements ManuallyOrdered {

	public static final int TITLE_MAX_LENGTH = 200;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	// Associação unidirecional e sem cascata: os filhos são consultados explicitamente e em ordem.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

	@Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
	private String title;

	@Column(name = "position", nullable = false)
	private int position;

	protected CourseModule() {}

	/**
	 * Cria um módulo no curso informado, na posição já calculada pelo serviço.
	 *
	 * <p>Exemplo: {@code new CourseModule(course, "Introdução", 1)}.
	 */
	public CourseModule(Course course, String title, int position) {
		this.course = course;
		this.title = CatalogTitle.requireValid(title, TITLE_MAX_LENGTH, "module title");
		this.position = ManuallyOrdered.requirePositive(position);
	}

	@Override
	public void moveToPosition(int newPosition) {
		this.position = ManuallyOrdered.requirePositive(newPosition);
	}

	@Override
	public UUID getId() {
		return id;
	}

	public Course getCourse() {
		return course;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public int getPosition() {
		return position;
	}

}
