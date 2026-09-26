import { Link } from 'react-router'
import type { CourseSummary } from '../api/types'
import { listVisibleCourses } from './courseLoaders'
import { useApiResource } from '../data/useApiResource'
import { useCurrentUser } from '../session/SessionContext'
import { CourseStatusBadge } from '../ui/CourseStatusBadge'
import { EmptyState } from '../ui/Feedback'
import { ResourceView } from '../ui/ResourceView'

/**
 * Cursos visíveis para o papel atual (GET /api/courses): MEMBER só vê publicados.
 *
 * Exemplo: rota `/cursos`.
 */
export function CourseListPage() {
  const { state, reload } = useApiResource(listVisibleCourses)
  return (
    <section aria-labelledby="courses-title">
      <h1 id="courses-title">Cursos</h1>
      <ResourceView state={state} loadingLabel="Carregando cursos…" onRetry={reload} notFoundBackTo="/cursos">
        {(courses) => <CourseCards courses={courses} />}
      </ResourceView>
    </section>
  )
}

function CourseCards({ courses }: { courses: CourseSummary[] }) {
  const user = useCurrentUser()
  if (courses.length === 0) {
    return (
      <EmptyState title="Nenhum curso disponível ainda">
        {user.role === 'ADMIN' ? 'Crie e publique um curso em "Organizar cursos".' : 'Volte mais tarde.'}
      </EmptyState>
    )
  }
  return (
    <ul className="card-list">
      {courses.map((course) => (
        <li key={course.id} className="card">
          <h2 className="card-title">
            <Link to={`/cursos/${course.id}`}>{course.title}</Link>
          </h2>
          {user.role === 'ADMIN' && <CourseStatusBadge status={course.status} />}
          {course.description && <p className="card-text">{course.description}</p>}
        </li>
      ))}
    </ul>
  )
}
