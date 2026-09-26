import { useCallback, useState } from 'react'
import { Link, useParams } from 'react-router'
import { useLibraryApi } from '../api/LibraryApiContext'
import type { CourseOutline } from '../api/types'
import { useCourseOutline } from '../catalog/useCourseOutline'
import { usePendingAction } from '../data/usePendingAction'
import { CourseStatusBadge } from '../ui/CourseStatusBadge'
import { Notice } from '../ui/Feedback'
import { ResourceView } from '../ui/ResourceView'
import { AddTitleForm } from './AddTitleForm'
import { MODULE_TITLE_MAX } from './catalogRules'
import { ModuleEditorList } from './ModuleEditorList'
import { PublishPanel } from './PublishPanel'
import { useCatalogReorder } from './useCatalogReorder'

/**
 * Organização de um curso: módulos, aulas, ordem e publicação. Sem edição ou exclusão: a API não tem
 * esses endpoints neste marco.
 *
 * Exemplo: rota `/admin/cursos/:courseId`.
 */
export function AdminCourseEditorPage() {
  const { courseId = '' } = useParams()
  const { state, reload } = useCourseOutline(courseId)
  return (
    <ResourceView state={state} loadingLabel="Carregando curso…" onRetry={reload} notFoundBackTo="/admin/cursos">
      {(outline) => <CourseEditor outline={outline} reload={reload} />}
    </ResourceView>
  )
}

function CourseEditor({ outline, reload }: { outline: CourseOutline; reload: () => void }) {
  const [announcement, setAnnouncement] = useState<string | null>(null)
  const onChanged = useCallback(
    (message: string | null) => {
      setAnnouncement(message)
      reload()
    },
    [reload],
  )
  const reorder = useCatalogReorder(reload, setAnnouncement)
  return (
    <article aria-labelledby="editor-title">
      <p className="breadcrumb">
        <Link to="/admin/cursos">Organizar cursos</Link>
      </p>
      <h1 id="editor-title">{outline.title}</h1>
      <p className="inline-actions">
        <CourseStatusBadge status={outline.status} />
        <Link to={`/cursos/${outline.id}`}>Ver como no catálogo</Link>
      </p>
      <div role="status" className="live-region">
        {announcement && <Notice tone="success" live={false}>{announcement}</Notice>}
      </div>
      <PublishPanel outline={outline} onChanged={onChanged} />
      <section className="panel" aria-labelledby="structure-title">
        <h2 id="structure-title">Módulos e aulas</h2>
        <ModuleEditorList outline={outline} reorder={reorder} onChanged={onChanged} />
        <AddModuleSection outline={outline} onChanged={onChanged} />
      </section>
    </article>
  )
}

function AddModuleSection({ outline, onChanged }: { outline: CourseOutline; onChanged: (message: string | null) => void }) {
  const api = useLibraryApi()
  const append = usePendingAction({ expireSessionOn401: true })
  // A API recusa (409) módulo novo em curso publicado: ele nasceria sem aulas e quebraria a publicação.
  if (outline.status === 'PUBLISHED') {
    return <p className="muted">Curso publicado não aceita novos módulos. Aulas ainda podem ser adicionadas.</p>
  }
  const submit = async (title: string) => {
    const outcome = await append.run(() => api.appendModule(outline.id, title))
    if (outcome.ok) onChanged(`Módulo "${title}" adicionado.`)
    return outcome.ok
  }
  return (
    <AddTitleForm
      label="Novo módulo"
      submitLabel="Adicionar módulo"
      maxLength={MODULE_TITLE_MAX}
      action={append}
      errorMessages={{ 409: 'Este curso já foi publicado e não aceita novos módulos.' }}
      onSubmit={submit}
    />
  )
}
