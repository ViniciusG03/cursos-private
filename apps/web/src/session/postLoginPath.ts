import type { UserRole } from '../api/types'

// Só áreas protegidas internas voltam após o login; páginas com token, login e URLs externas não.
const PROTECTED_AREA = /^\/(cursos|admin)(\/[A-Za-z0-9\-._~%/]*)?$/

/**
 * Página inicial de cada papel.
 *
 * Exemplo: `homePathFor('ADMIN') === '/admin/cursos'`.
 */
export function homePathFor(role: UserRole): string {
  return role === 'ADMIN' ? '/admin/cursos' : '/cursos'
}

/**
 * Escolhe para onde ir após o login: a rota interna pedida, se for segura e permitida ao papel;
 * senão a página inicial do papel. Nunca devolve URL de outro domínio.
 *
 * Exemplo: `resolvePostLoginPath('//evil.com', 'MEMBER') === '/cursos'`.
 */
export function resolvePostLoginPath(requested: unknown, role: UserRole): string {
  const home = homePathFor(role)
  if (typeof requested !== 'string' || !PROTECTED_AREA.test(requested) || requested.includes('//')) {
    return home
  }
  if (requested.startsWith('/admin') && role !== 'ADMIN') {
    return home
  }
  return requested
}
