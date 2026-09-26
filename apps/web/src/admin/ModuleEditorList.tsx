import { useLibraryApi } from '../api/LibraryApiContext'
import type { CourseOutline, ModuleOutline } from '../api/types'
import { usePendingAction } from '../data/usePendingAction'
import { describeApiError } from '../ui/describeApiError'
import { EmptyState, Notice } from '../ui/Feedback'
import { AddTitleForm } from './AddTitleForm'
import { LESSON_TITLE_MAX } from './catalogRules'
import { ReorderButtons } from './ReorderButtons'
import { focusFor } from './useCatalogReorder'
import type { CatalogReorder } from './useCatalogReorder'

interface ModuleEditorListProps {
  outline: CourseOutline
  reorder: CatalogReorder
  onChanged: (message: string | null) => void
}

const REORDER_MESSAGES = {
  400: 'A ordem enviada não corresponde aos itens atuais. A ordem salva foi recarregada.',
  404: 'O curso ou módulo não existe mais. A página foi recarregada.',
}

/**
 * Módulos do curso com reordenação e, dentro de cada um, aulas com reordenação e inclusão.
 *
 * Exemplo: `<ModuleEditorList outline={outline} reorder={reorder} onChanged={announceAndReload} />`.
 */
export function ModuleEditorList({ outline, reorder, onChanged }: ModuleEditorListProps) {
  if (outline.modules.length === 0) {
    return <EmptyState title="Nenhum módulo ainda">Adicione o primeiro módulo abaixo.</EmptyState>
  }
  return (
    <>
      {reorder.action.error !== null && (
        <Notice tone="error">{describeApiError(reorder.action.error, REORDER_MESSAGES)}</Notice>
      )}
      <ol className="module-list editor">
        {outline.modules.map((module, index) => (
          <li key={module.id} className="module">
            <div className="item-row">
              <h3 className="module-title">
                {index + 1}. {module.title}
              </h3>
              <ReorderButtons
                itemLabel={`módulo ${module.title}`}
                index={index}
                count={outline.modules.length}
                disabled={reorder.action.pending}
                focusRequested={focusFor(reorder.focusRequest, module.id, index)}
                onFocusHandled={reorder.clearFocusRequest}
                onMove={(direction) => reorder.move({ kind: 'modules', courseId: outline.id }, outline.modules, index, direction)}
              />
            </div>
            <LessonEditorList module={module} reorder={reorder} />
            <AddLessonForm module={module} onChanged={onChanged} />
          </li>
        ))}
      </ol>
    </>
  )
}

function LessonEditorList({ module, reorder }: { module: ModuleOutline; reorder: CatalogReorder }) {
  if (module.lessons.length === 0) {
    return <p className="muted">Sem aulas: um módulo vazio impede a publicação.</p>
  }
  return (
    <ol className="lesson-list editor">
      {module.lessons.map((lesson, index) => (
        <li key={lesson.id} className="item-row">
          <span>
            {index + 1}. {lesson.title}
          </span>
          <ReorderButtons
            itemLabel={`aula ${lesson.title}`}
            index={index}
            count={module.lessons.length}
            disabled={reorder.action.pending}
            focusRequested={focusFor(reorder.focusRequest, lesson.id, index)}
            onFocusHandled={reorder.clearFocusRequest}
            onMove={(direction) => reorder.move({ kind: 'lessons', moduleId: module.id }, module.lessons, index, direction)}
          />
        </li>
      ))}
    </ol>
  )
}

// A API aceita aula nova também em curso publicado (não quebra a completude), então o formulário fica sempre.
function AddLessonForm({ module, onChanged }: { module: ModuleOutline; onChanged: (message: string | null) => void }) {
  const api = useLibraryApi()
  const append = usePendingAction({ expireSessionOn401: true })
  const submit = async (title: string) => {
    const outcome = await append.run(() => api.appendLesson(module.id, title))
    if (outcome.ok) onChanged(`Aula "${title}" adicionada ao módulo "${module.title}".`)
    return outcome.ok
  }
  return (
    <AddTitleForm
      label={`Nova aula em "${module.title}"`}
      submitLabel="Adicionar aula"
      maxLength={LESSON_TITLE_MAX}
      action={append}
      errorMessages={{ 404: 'Este módulo não existe mais. Recarregue a página.' }}
      onSubmit={submit}
    />
  )
}
