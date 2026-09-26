package dev.vinicius.cursos.api.catalog.service;

import dev.vinicius.cursos.api.catalog.domain.ManuallyOrdered;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reordena filhos de um mesmo pai respeitando as constraints {@code UNIQUE (pai, position)} de
 * V2/V3, que são verificadas imediatamente (não deferrable): uma troca direta de posições falharia
 * no PostgreSQL. Por isso a mudança acontece em duas fases com flush entre elas.
 */
@Component
class ManualPositionReorderer {

	private final EntityManager entityManager;

	ManualPositionReorderer(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * Valida a lista completa e deixa os itens nas posições 1..N na ordem pedida. Precisa rodar dentro
	 * da transação do serviço chamador para que uma falha desfaça as duas fases.
	 *
	 * <p>Exemplo: {@code reorderer.reorder("course", courseId, modules, List.of(b, a))}.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	<T extends ManuallyOrdered> void reorder(String parentKind, UUID parentId, List<T> currentItems,
			List<UUID> orderedIds) {
		List<UUID> currentIds = currentItems.stream().map(ManuallyOrdered::getId).toList();
		CompleteIdListValidator.requireSameIds(parentKind, parentId, currentIds, orderedIds);
		List<T> orderedItems = arrangeByIds(currentItems, orderedIds);
		// Fase 1: posições temporárias acima de todas as atuais. CHECK (position > 0) impede usar negativos.
		assignSequentialPositions(orderedItems, findLastPosition(currentItems));
		entityManager.flush();
		// Fase 2: como todas estão acima de N, 1..N não colide em nenhuma ordem de UPDATE.
		assignSequentialPositions(orderedItems, 0);
		entityManager.flush();
	}

	private static <T extends ManuallyOrdered> List<T> arrangeByIds(List<T> items, List<UUID> orderedIds) {
		Map<UUID, T> itemsById = items.stream().collect(Collectors.toMap(ManuallyOrdered::getId, Function.identity()));
		return orderedIds.stream().map(itemsById::get).toList();
	}

	private static int findLastPosition(List<? extends ManuallyOrdered> items) {
		return items.stream().mapToInt(ManuallyOrdered::getPosition).max().orElse(0);
	}

	private static void assignSequentialPositions(List<? extends ManuallyOrdered> orderedItems, int offset) {
		for (int index = 0; index < orderedItems.size(); index++) {
			orderedItems.get(index).moveToPosition(offset + index + 1);
		}
	}

}
