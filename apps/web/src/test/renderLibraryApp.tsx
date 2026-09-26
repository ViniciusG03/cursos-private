import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { takeLinkToken } from '../access/linkToken'
import { LibraryApp } from '../app/LibraryApp'
import type { CourseOutline } from '../api/types'
import type { FakeLibraryApi } from './FakeLibraryApi'

/**
 * Abre a SPA em `url` como o navegador faria (inclusive o fragmento), passando pela mesma captura de
 * token de main.tsx.
 *
 * Exemplo: `const { user } = renderLibraryApp(api, '/convites/aceitar#token=abc')`.
 */
export function renderLibraryApp(api: FakeLibraryApi, url: string) {
  window.history.replaceState(null, '', url)
  const linkToken = takeLinkToken(window.location, window.history)
  const user = userEvent.setup()
  const view = render(<LibraryApp api={api} linkToken={linkToken} />)
  return { user, ...view }
}

/** Contas padrão dos testes. */
export const ADMIN_EMAIL = 'admin@exemplo.com'
export const MEMBER_EMAIL = 'membro@exemplo.com'
export const PASSWORD = 'uma frase longa o bastante'

/**
 * Curso publicado com dois módulos e três aulas, na ordem da API.
 *
 * Exemplo: `const course = api.addCourse(publishedCourseFixture())`.
 */
export function publishedCourseFixture(): Omit<CourseOutline, 'id'> {
  return {
    title: 'Java moderno',
    description: 'Do básico aos records.',
    status: 'PUBLISHED',
    modules: [
      {
        id: 'module-intro',
        title: 'Introdução',
        position: 1,
        lessons: [
          { id: 'lesson-setup', title: 'Instalando o JDK', position: 1 },
          { id: 'lesson-hello', title: 'Olá, mundo', position: 2 },
        ],
      },
      { id: 'module-records', title: 'Records', position: 2, lessons: [{ id: 'lesson-records', title: 'Records na prática', position: 1 }] },
    ],
  }
}
