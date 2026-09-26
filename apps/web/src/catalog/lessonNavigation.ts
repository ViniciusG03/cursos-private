import type { CourseOutline, LessonOutline, ModuleOutline } from '../api/types'

/** Aula localizada no curso carregado, com posições para exibição e vizinhas na ordem do curso. */
export interface LocatedLesson {
  lesson: LessonOutline
  module: ModuleOutline
  moduleNumber: number
  lessonNumber: number
  previous: LessonOutline | null
  next: LessonOutline | null
}

/**
 * Procura a aula no curso já carregado; null se ela não pertence a ele (a tela mostra 404).
 * A ordem é a recebida da API (position), sem reordenar no cliente.
 *
 * Exemplo: `locateLesson(outline, lessonId)?.module.title`.
 */
export function locateLesson(outline: CourseOutline, lessonId: string): LocatedLesson | null {
  const sequence = outline.modules.flatMap((module) => module.lessons)
  const index = sequence.findIndex((lesson) => lesson.id === lessonId)
  if (index < 0) {
    return null
  }
  const moduleIndex = outline.modules.findIndex((module) => module.lessons.some((lesson) => lesson.id === lessonId))
  const module = outline.modules[moduleIndex]
  return {
    lesson: sequence[index],
    module,
    moduleNumber: moduleIndex + 1,
    lessonNumber: module.lessons.findIndex((lesson) => lesson.id === lessonId) + 1,
    previous: sequence[index - 1] ?? null,
    next: sequence[index + 1] ?? null,
  }
}

/**
 * Total de aulas do curso.
 *
 * Exemplo: `countLessons(outline) === 0` indica curso sem aulas.
 */
export function countLessons(outline: CourseOutline): number {
  return outline.modules.reduce((total, module) => total + module.lessons.length, 0)
}
