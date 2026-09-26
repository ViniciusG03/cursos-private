import { Link, useParams } from 'react-router'
import type { CourseOutline } from '../api/types'
import { NotFoundContent, Notice } from '../ui/Feedback'
import { ResourceView } from '../ui/ResourceView'
import { locateLesson } from './lessonNavigation'
import type { LocatedLesson } from './lessonNavigation'
import { useCourseOutline } from './useCourseOutline'

/**
 * Aula selecionada com a posição no curso. O vídeo entra no marco 5: aqui não há player nem progresso.
 *
 * Exemplo: rota `/cursos/:courseId/aulas/:lessonId`.
 */
export function LessonPage() {
  const { courseId = '', lessonId = '' } = useParams()
  const { state, reload } = useCourseOutline(courseId)
  return (
    <ResourceView state={state} loadingLabel="Carregando aula…" onRetry={reload} notFoundBackTo="/cursos">
      {(outline) => <LessonInCourse outline={outline} lessonId={lessonId} />}
    </ResourceView>
  )
}

function LessonInCourse({ outline, lessonId }: { outline: CourseOutline; lessonId: string }) {
  const located = locateLesson(outline, lessonId)
  if (located === null) {
    return <NotFoundContent backTo={`/cursos/${outline.id}`} backLabel="Voltar ao curso" />
  }
  return (
    <article aria-labelledby="lesson-title">
      <p className="breadcrumb">
        <Link to="/cursos">Cursos</Link> / <Link to={`/cursos/${outline.id}`}>{outline.title}</Link>
      </p>
      <p className="eyebrow">
        Módulo {located.moduleNumber} de {outline.modules.length}: {located.module.title}
      </p>
      <h1 id="lesson-title">{located.lesson.title}</h1>
      <p className="muted">
        Aula {located.lessonNumber} de {located.module.lessons.length} neste módulo
      </p>
      <Notice tone="info">O conteúdo em vídeo desta aula ainda não está disponível.</Notice>
      <LessonPager courseId={outline.id} located={located} />
    </article>
  )
}

function LessonPager({ courseId, located }: { courseId: string; located: LocatedLesson }) {
  return (
    <nav className="lesson-pager" aria-label="Aulas vizinhas">
      {located.previous && (
        <Link to={`/cursos/${courseId}/aulas/${located.previous.id}`}>← Anterior: {located.previous.title}</Link>
      )}
      {located.next && <Link to={`/cursos/${courseId}/aulas/${located.next.id}`}>Próxima: {located.next.title} →</Link>}
    </nav>
  )
}
