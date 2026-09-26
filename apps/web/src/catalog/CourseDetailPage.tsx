import { Link, useParams } from 'react-router'
import type { CourseOutline } from '../api/types'
import { useCurrentUser } from '../session/SessionContext'
import { CourseStatusBadge } from '../ui/CourseStatusBadge'
import { EmptyState } from '../ui/Feedback'
import { ResourceView } from '../ui/ResourceView'
import { countLessons } from './lessonNavigation'
import { useCourseOutline } from './useCourseOutline'

/**
 * Módulos e aulas de um curso na ordem recebida da API.
 *
 * Exemplo: rota `/cursos/:courseId`.
 */
export function CourseDetailPage() {
  const { courseId = '' } = useParams()
  const { state, reload } = useCourseOutline(courseId)
  return (
    <ResourceView state={state} loadingLabel="Carregando curso…" onRetry={reload} notFoundBackTo="/cursos">
      {(outline) => <CourseOutlineView outline={outline} />}
    </ResourceView>
  )
}

function CourseOutlineView({ outline }: { outline: CourseOutline }) {
  const user = useCurrentUser()
  return (
    <article aria-labelledby="course-title">
      <p className="breadcrumb">
        <Link to="/cursos">Cursos</Link>
      </p>
      <h1 id="course-title">{outline.title}</h1>
      {user.role === 'ADMIN' && (
        <p className="inline-actions">
          <CourseStatusBadge status={outline.status} />
          <Link to={`/admin/cursos/${outline.id}`}>Organizar este curso</Link>
        </p>
      )}
      {outline.description && <p className="lead">{outline.description}</p>}
      {countLessons(outline) === 0 ? (
        <EmptyState title="Este curso ainda não tem aulas" />
      ) : (
        <ModuleList outline={outline} />
      )}
    </article>
  )
}

function ModuleList({ outline }: { outline: CourseOutline }) {
  return (
    <ol className="module-list">
      {outline.modules.map((module) => (
        <li key={module.id} className="module">
          <h2 className="module-title">{module.title}</h2>
          {module.lessons.length === 0 ? (
            <p className="muted">Módulo sem aulas.</p>
          ) : (
            <ol className="lesson-list">
              {module.lessons.map((lesson) => (
                <li key={lesson.id}>
                  <Link to={`/cursos/${outline.id}/aulas/${lesson.id}`}>{lesson.title}</Link>
                </li>
              ))}
            </ol>
          )}
        </li>
      ))}
    </ol>
  )
}
