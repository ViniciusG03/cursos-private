import type { CourseOutline } from '../api/types'

// Limites de CatalogTitle na API: Course.TITLE_MAX_LENGTH = 250; módulo e aula = 200.
export const COURSE_TITLE_MAX = 250
export const MODULE_TITLE_MAX = 200
export const LESSON_TITLE_MAX = 200

/** Sentido de um movimento na lista ordenada. */
export type MoveDirection = 'up' | 'down'

/**
 * Valida um título como a API (aparado, não vazio, até `maxLength`); null quando válido.
 *
 * Exemplo: `validateCatalogTitle('  ', 200) === 'Informe um título.'`.
 */
export function validateCatalogTitle(title: string, maxLength: number): string | null {
  const trimmed = title.trim()
  if (trimmed === '') {
    return 'Informe um título.'
  }
  if (trimmed.length > maxLength) {
    return `O título pode ter até ${maxLength} caracteres; tem ${trimmed.length}.`
  }
  return null
}

/**
 * Nova lista completa de IDs com o item em `index` trocado de lugar com o vizinho. A API recebe
 * sempre a lista inteira.
 *
 * Exemplo: `moveId(['a', 'b', 'c'], 2, 'up')` devolve `['a', 'c', 'b']`.
 */
export function moveId(ids: readonly string[], index: number, direction: MoveDirection): string[] {
  const target = direction === 'up' ? index - 1 : index + 1
  if (index < 0 || index >= ids.length || target < 0 || target >= ids.length) {
    throw new RangeError(`cannot move index ${index} ${direction} in a list of ${ids.length}: expected a neighbour`)
  }
  const reordered = [...ids]
  ;[reordered[index], reordered[target]] = [reordered[target], reordered[index]]
  return reordered
}

/**
 * Motivos pelos quais a API recusaria publicar (409), com os títulos dos módulos em vez dos UUIDs
 * da mensagem da API. Lista vazia quando a estrutura está completa.
 *
 * Exemplo: `describePublishBlockers(outline)` → `['Todo módulo precisa de ao menos uma aula. Sem aulas: Intro.']`.
 */
export function describePublishBlockers(outline: CourseOutline): string[] {
  if (outline.modules.length === 0) {
    return ['O curso precisa de ao menos um módulo.']
  }
  const emptyModules = outline.modules.filter((module) => module.lessons.length === 0)
  if (emptyModules.length === 0) {
    return []
  }
  const titles = emptyModules.map((module) => module.title).join(', ')
  return [`Todo módulo precisa de ao menos uma aula. Sem aulas: ${titles}.`]
}
