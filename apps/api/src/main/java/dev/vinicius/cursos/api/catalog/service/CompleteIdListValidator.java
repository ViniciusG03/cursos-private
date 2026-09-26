package dev.vinicius.cursos.api.catalog.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Garante que uma lista de reordenação contém exatamente os IDs atuais do pai, cada um uma vez.
 * Roda antes de qualquer mudança de posição, para que uma lista inválida não altere nada.
 */
final class CompleteIdListValidator {

	private CompleteIdListValidator() {}

	/**
	 * Rejeita IDs duplicados, estranhos ao pai (de outro pai ou inexistentes) e listas parciais.
	 *
	 * <p>Exemplo: {@code requireSameIds("course", courseId, List.of(a, b), List.of(b, a))}.
	 */
	static void requireSameIds(String parentKind, UUID parentId, List<UUID> currentIds, List<UUID> requestedIds) {
		if (requestedIds == null) {
			throw new InvalidReorderException(
					"reorder of %s %s expects the complete list of child ids, got null".formatted(parentKind, parentId));
		}
		Set<UUID> duplicatedIds = findDuplicatedIds(requestedIds);
		Set<UUID> foreignIds = subtract(requestedIds, currentIds);
		Set<UUID> missingIds = subtract(currentIds, requestedIds);
		if (duplicatedIds.isEmpty() && foreignIds.isEmpty() && missingIds.isEmpty()) {
			return;
		}
		throw new InvalidReorderException(
				"reorder of %s %s expects each current child id exactly once %s; duplicated=%s, not in this %s=%s, missing=%s"
					.formatted(parentKind, parentId, currentIds, duplicatedIds, parentKind, foreignIds, missingIds));
	}

	private static Set<UUID> findDuplicatedIds(List<UUID> requestedIds) {
		Set<UUID> seenIds = new HashSet<>();
		Set<UUID> duplicatedIds = new LinkedHashSet<>();
		for (UUID requestedId : requestedIds) {
			if (!seenIds.add(requestedId)) {
				duplicatedIds.add(requestedId);
			}
		}
		return duplicatedIds;
	}

	private static Set<UUID> subtract(List<UUID> sourceIds, List<UUID> idsToRemove) {
		Set<UUID> remainingIds = new LinkedHashSet<>(sourceIds);
		remainingIds.removeAll(new HashSet<>(idsToRemove));
		return remainingIds;
	}

}
