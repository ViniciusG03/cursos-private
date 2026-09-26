import { useCallback } from 'react'
import type { LibraryApi } from '../api/LibraryApi'
import type { CourseOutline } from '../api/types'
import { useApiResource } from '../data/useApiResource'

/**
 * Curso com módulos e aulas (GET /api/courses/{id}); rascunho para MEMBER chega como 404 da API.
 *
 * Exemplo: `const { state, reload } = useCourseOutline(courseId)`.
 */
export function useCourseOutline(courseId: string) {
  const loader = useCallback((api: LibraryApi): Promise<CourseOutline> => api.readCourse(courseId), [courseId])
  return useApiResource(loader)
}
