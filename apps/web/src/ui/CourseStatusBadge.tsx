import type { CourseStatus } from '../api/types'

const LABELS: Record<CourseStatus, string> = { DRAFT: 'Rascunho', PUBLISHED: 'Publicado' }

/**
 * Situação do curso em texto (não só cor).
 *
 * Exemplo: `<CourseStatusBadge status="DRAFT" />` mostra "Rascunho".
 */
export function CourseStatusBadge({ status }: { status: CourseStatus }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{LABELS[status]}</span>
}
