package dev.vinicius.cursos.api.catalog.domain;

import java.util.UUID;

/**
 * Item do catálogo com posição manual (1..N) dentro do seu pai: módulos no curso, aulas no módulo.
 *
 * <p>Exemplo: {@code lesson.moveToPosition(3)}.
 */
public interface ManuallyOrdered {

	UUID getId();

	int getPosition();

	/** Altera a posição; valores não positivos são rejeitados como no {@code CHECK} do banco. */
	void moveToPosition(int newPosition);

	/**
	 * Valida uma posição antes de atribuí-la, espelhando {@code CHECK (position > 0)} de V2/V3.
	 *
	 * <p>Exemplo: {@code int position = ManuallyOrdered.requirePositive(1);}
	 */
	static int requirePositive(int position) {
		if (position <= 0) {
			throw new IllegalArgumentException("position must be a positive integer (>= 1), got: " + position);
		}
		return position;
	}

}
