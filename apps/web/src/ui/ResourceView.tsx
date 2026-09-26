import type { ReactNode } from 'react'
import { isSessionExpired } from '../api/ApiError'
import type { ResourceState } from '../data/useApiResource'
import { isMissingResource } from './describeApiError'
import { ErrorState, LoadingState, NotFoundContent } from './Feedback'

interface ResourceViewProps<T> {
  state: ResourceState<T>
  loadingLabel: string
  onRetry: () => void
  notFoundBackTo: string
  children: (value: T) => ReactNode
}

/**
 * Renderiza carregando, 404 (inclusive ID malformado), erro com nova tentativa ou o conteúdo. Um 401
 * não mostra nada: a sessão local já foi encerrada e o guarda leva ao login.
 *
 * Exemplo: `<ResourceView state={state} ...>{(courses) => <CourseList courses={courses} />}</ResourceView>`.
 */
export function ResourceView<T>({ state, loadingLabel, onRetry, notFoundBackTo, children }: ResourceViewProps<T>) {
  if (state.status === 'loading') {
    return <LoadingState label={loadingLabel} />
  }
  if (state.status === 'loaded') {
    return <>{children(state.value)}</>
  }
  if (isSessionExpired(state.error)) {
    return null
  }
  if (isMissingResource(state.error)) {
    return <NotFoundContent backTo={notFoundBackTo} backLabel="Voltar à lista de cursos" />
  }
  return <ErrorState error={state.error} onRetry={onRetry} />
}
