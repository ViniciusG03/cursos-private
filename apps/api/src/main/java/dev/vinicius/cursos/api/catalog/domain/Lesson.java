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
 * Aula de um módulo, mapeada sobre {@code lessons} (V3). Vídeo e progresso ficam fora desta
 * entidade de propósito: entram em marcos futuros e em tabelas próprias.
 */
@Entity
@Table(name = "lessons")
public class Lesson implements ManuallyOrdered {

	public static final int TITLE_MAX_LENGTH = 200;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "module_id", nullable = false)
	private CourseModule module;

	@Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
	private String title;

	@Column(name = "position", nullable = false)
	private int position;

	protected Lesson() {}

	/**
	 * Cria uma aula no módulo informado, na posição já calculada pelo serviço.
	 *
	 * <p>Exemplo: {@code new Lesson(module, "Instalando o JDK", 1)}.
	 */
	public Lesson(CourseModule module, String title, int position) {
		this.module = module;
		this.title = CatalogTitle.requireValid(title, TITLE_MAX_LENGTH, "lesson title");
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

	public CourseModule getModule() {
		return module;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public int getPosition() {
		return position;
	}

}
