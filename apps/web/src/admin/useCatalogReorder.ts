import { useCallback, useState } from 'react'
import { useLibraryApi } from '../api/LibraryApiContext'
import { usePendingAction } from '../data/usePendingAction'
import type { PendingAction } from '../data/usePendingAction'
import { moveId } from './catalogRules'
import type { MoveDirection } from './catalogRules'

/** O que está sendo reordenado: módulos de um curso ou aulas de um módulo. */
export type ReorderScope = { kind: 'modules'; courseId: string } | { kind: 'lessons'; moduleId: string }

/** Pedido de foco para o botão do item depois que ele aparecer na posição esperada. */
export interface ReorderFocusRequest {
  itemId: string
  direction: MoveDirection
  expectedIndex: number
}

/** Um item da lista a mover. */
export interface ReorderItem {
  id: string
  title: string
}

export interface CatalogReorder {
  action: PendingAction
  focusRequest: ReorderFocusRequest | null
  clearFocusRequest: () => void
  move: (scope: ReorderScope, items: ReorderItem[], index: number, direction: MoveDirection) => Promise<void>
}

/**
 * Reordena enviando a lista completa de IDs ao PUT correspondente. Com sucesso ou falha, recarrega o
 * curso para mostrar a ordem confirmada pelo servidor; nunca mantém uma ordem só local.
 *
 * Exemplo: `reorder.move({ kind: 'modules', courseId }, outline.modules, 1, 'up')`.
 */
export function useCatalogReorder(reload: () => void, announce: (message: string) => void): CatalogReorder {
  const api = useLibraryApi()
  const action = usePendingAction({ expireSessionOn401: true })
  const [focusRequest, setFocusRequest] = useState<ReorderFocusRequest | null>(null)

  const move = async (scope: ReorderScope, items: ReorderItem[], index: number, direction: MoveDirection) => {
    if (action.pending) return
    const ids = moveId(
      items.map((item) => item.id),
      index,
      direction,
    )
    const expectedIndex = direction === 'up' ? index - 1 : index + 1
    setFocusRequest({ itemId: items[index].id, direction, expectedIndex })
    const outcome = await action.run(() => sendOrder(scope, ids))
    if (outcome.ok) announce(`"${items[index].title}" agora está na posição ${expectedIndex + 1}.`)
    else setFocusRequest({ itemId: items[index].id, direction, expectedIndex: index })
    reload()
  }
  const sendOrder = (scope: ReorderScope, ids: string[]) =>
    scope.kind === 'modules' ? api.reorderModules(scope.courseId, ids) : api.reorderLessons(scope.moduleId, ids)
  const clearFocusRequest = useCallback(() => setFocusRequest(null), [])
  return { action, focusRequest, clearFocusRequest, move }
}

/**
 * Direção de foco para um item, somente quando ele já está na posição esperada.
 *
 * Exemplo: `focusFor(reorder.focusRequest, module.id, index)`.
 */
export function focusFor(request: ReorderFocusRequest | null, itemId: string, index: number): MoveDirection | null {
  return request !== null && request.itemId === itemId && request.expectedIndex === index ? request.direction : null
}
