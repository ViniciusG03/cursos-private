import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { useLibraryApi } from '../api/LibraryApiContext'
import type { CourseSummary } from '../api/types'
import { listVisibleCourses } from '../catalog/courseLoaders'
import { useApiResource } from '../data/useApiResource'
import { usePendingAction } from '../data/usePendingAction'
import { CourseStatusBadge } from '../ui/CourseStatusBadge'
import { describeApiError } from '../ui/describeApiError'
import { EmptyState, Notice } from '../ui/Feedback'
import { SubmitButton, TextField } from '../ui/FormControls'
import { ResourceView } from '../ui/ResourceView'
import { COURSE_TITLE_MAX, validateCatalogTitle } from './catalogRules'

/**
 * Lista de rascunhos e publicados com criação de rascunho (POST /api/admin/courses).
 *
 * Exemplo: rota `/admin/cursos`.
 */
export function AdminCoursesPage() {
  const { state, reload } = useApiResource(listVisibleCourses)
  return (
    <section aria-labelledby="admin-courses-title">
      <h1 id="admin-courses-title">Organizar cursos</h1>
      <CreateCourseForm />
      <h2>Cursos existentes</h2>
      <ResourceView state={state} loadingLabel="Carregando cursos…" onRetry={reload} notFoundBackTo="/admin/cursos">
        {(courses) => <AdminCourseTable courses={courses} />}
      </ResourceView>
    </section>
  )
}

function AdminCourseTable({ courses }: { courses: CourseSummary[] }) {
  if (courses.length === 0) {
    return <EmptyState title="Nenhum curso criado">Crie o primeiro rascunho acima.</EmptyState>
  }
  return (
    <ul className="card-list">
      {courses.map((course) => (
        <li key={course.id} className="card">
          <h3 className="card-title">
            <Link to={`/admin/cursos/${course.id}`}>{course.title}</Link>
          </h3>
          <CourseStatusBadge status={course.status} />
          {course.description && <p className="card-text">{course.description}</p>}
        </li>
      ))}
    </ul>
  )
}

function CreateCourseForm() {
  const api = useLibraryApi()
  const navigate = useNavigate()
  const create = usePendingAction({ expireSessionOn401: true })
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [titleError, setTitleError] = useState<string | null>(null)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const problem = validateCatalogTitle(title, COURSE_TITLE_MAX)
    setTitleError(problem)
    if (problem !== null) return
    const course = { title: title.trim(), description: description.trim() === '' ? null : description.trim() }
    const outcome = await create.run(() => api.createCourse(course))
    if (outcome.ok) navigate(`/admin/cursos/${outcome.value.id}`)
  }

  return (
    <form className="panel" onSubmit={handleSubmit} noValidate aria-labelledby="create-course-title">
      <h2 id="create-course-title">Novo curso</h2>
      {create.error !== null && <Notice tone="error">{describeApiError(create.error)}</Notice>}
      <TextField label="Título" value={title} onValueChange={setTitle} error={titleError} maxLength={COURSE_TITLE_MAX} />
      <TextField label="Descrição (opcional)" multiline value={description} onValueChange={setDescription} />
      <SubmitButton pending={create.pending} pendingLabel="Criando…">
        Criar rascunho
      </SubmitButton>
    </form>
  )
}
