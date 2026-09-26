import { useCallback, useRef, useState } from 'react'
import { isSessionExpired } from '../api/ApiError'
import { useSession } from '../session/SessionContext'

/** Resultado de uma ação: valor em caso de sucesso; o erro fica em `error` do hook. */
export type ActionOutcome<T> = { ok: true; value: T } | { ok: false }

export interface PendingAction {
  pending: boolean
  error: unknown
  run<T>(action: () => Promise<T>): Promise<ActionOutcome<T>>
  clearError(): void
}

/**
 * Executa uma escrita por vez: chamadas enquanto outra está pendente são ignoradas (sem duplo envio)
 * e nada é repetido automaticamente, nem após 403. Em áreas autenticadas, 401 encerra a sessão local.
 *
 * Exemplo: `const publish = usePendingAction(); await publish.run(() => api.publishCourse(id))`.
 */
export function usePendingAction(options: { expireSessionOn401: boolean }): PendingAction {
  const { expire } = useSession()
  const pendingRef = useRef(false)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const { expireSessionOn401 } = options

  const run = useCallback(
    async <T>(action: () => Promise<T>): Promise<ActionOutcome<T>> => {
      if (pendingRef.current) return { ok: false }
      pendingRef.current = true
      setPending(true)
      setError(null)
      try {
        return { ok: true, value: await action() }
      } catch (failure) {
        if (expireSessionOn401 && isSessionExpired(failure)) expire()
        setError(failure)
        return { ok: false }
      } finally {
        pendingRef.current = false
        setPending(false)
      }
    },
    [expire, expireSessionOn401],
  )
  const clearError = useCallback(() => setError(null), [])
  return { pending, error, run, clearError }
}
