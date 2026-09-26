import { useCallback, useEffect, useState } from 'react'
import { isSessionExpired } from '../api/ApiError'
import type { LibraryApi } from '../api/LibraryApi'
import { useLibraryApi } from '../api/LibraryApiContext'
import { useSession } from '../session/SessionContext'

/** Estado de uma leitura: carregando, carregada ou falha (com o erro para a tela decidir). */
export type ResourceState<T> =
  | { status: 'loading' }
  | { status: 'loaded'; value: T }
  | { status: 'failed'; error: unknown }

type SettledState<T> = Exclude<ResourceState<T>, { status: 'loading' }>

/** Leitura da API; deve ser estável (useCallback) para não recarregar a cada render. */
export type ResourceLoader<T> = (api: LibraryApi) => Promise<T>

interface SettledResult<T> {
  loader: ResourceLoader<T>
  state: SettledState<T>
}

const LOADING = { status: 'loading' } as const

/**
 * Carrega um recurso ao montar e quando o loader muda. `reload()` busca de novo mantendo o valor
 * anterior visível até a resposta (evita perder foco ao reordenar). 401 encerra a sessão local.
 *
 * Exemplo: `const { state, reload } = useApiResource(useCallback((api) => api.listCourses(), []))`.
 */
export function useApiResource<T>(loader: ResourceLoader<T>): { state: ResourceState<T>; reload: () => void } {
  const api = useLibraryApi()
  const { expire } = useSession()
  const [result, setResult] = useState<SettledResult<T> | null>(null)
  const [round, setRound] = useState(0)

  useEffect(() => {
    let current = true
    const settle = (state: SettledState<T>) => current && setResult({ loader, state })
    loader(api).then(
      (value) => settle({ status: 'loaded', value }),
      (error: unknown) => {
        if (current && isSessionExpired(error)) expire()
        settle({ status: 'failed', error })
      },
    )
    return () => {
      current = false
    }
  }, [api, loader, round, expire])

  const reload = useCallback(() => setRound((value) => value + 1), [])
  // Um resultado de outro loader (ex.: curso anterior) não vale para a rota atual.
  const state = result !== null && result.loader === loader ? result.state : LOADING
  return { state, reload }
}
