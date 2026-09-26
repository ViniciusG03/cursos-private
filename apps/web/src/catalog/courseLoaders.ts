import type { LibraryApi } from '../api/LibraryApi'
import type { CourseSummary } from '../api/types'

/**
 * Leitura estável da lista de cursos para `useApiResource` (catálogo e administração).
 *
 * Exemplo: `const { state } = useApiResource(listVisibleCourses)`.
 */
export function listVisibleCourses(api: LibraryApi): Promise<CourseSummary[]> {
  return api.listCourses()
}
