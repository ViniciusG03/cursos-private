import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { isSessionExpired } from '../api/ApiError'
import { useLibraryApi } from '../api/LibraryApiContext'
import type { CurrentUser } from '../api/types'

/** Por que não há sessão: nunca houve, expirou no meio do uso ou a pessoa saiu. */
export type AnonymousReason = 'initial' | 'expired' | 'signedOut'

/** Estado da sessão, só em memória: nada vai para localStorage. */
export type SessionState =
  | { status: 'checking' }
  | { status: 'unavailable'; error: unknown }
  | { status: 'anonymous'; reason: AnonymousReason }
  | { status: 'authenticated'; user: CurrentUser }

export interface SessionControls {
  state: SessionState
  signIn(email: string, password: string): Promise<CurrentUser>
  signOut(): Promise<void>
  expire(): void
  recheck(): void
}

const SessionContext = createContext<SessionControls | null>(null)

/**
 * Consulta GET /api/auth/me ao montar e expõe login, logout e expiração da sessão.
 *
 * Exemplo: `<SessionProvider><AppRoutes /></SessionProvider>`.
 */
export function SessionProvider({ children }: { children: ReactNode }) {
  const api = useLibraryApi()
  const [state, setState] = useState<SessionState>({ status: 'checking' })
  const [checkRound, setCheckRound] = useState(0)

  useEffect(() => {
    let current = true
    api.currentUser().then(
      (user) => current && setState({ status: 'authenticated', user }),
      (error: unknown) => current && setState(anonymousOrUnavailable(error)),
    )
    return () => {
      current = false
    }
  }, [api, checkRound])

  const controls = useSessionControls(state, setState, () => setCheckRound((round) => round + 1))
  return <SessionContext.Provider value={controls}>{children}</SessionContext.Provider>
}

function anonymousOrUnavailable(error: unknown): SessionState {
  return isSessionExpired(error) ? { status: 'anonymous', reason: 'initial' } : { status: 'unavailable', error }
}

function useSessionControls(
  state: SessionState,
  setState: (next: SessionState) => void,
  restartCheck: () => void,
): SessionControls {
  const api = useLibraryApi()
  const signIn = useCallback(
    async (email: string, password: string) => {
      const user = await api.login(email, password)
      setState({ status: 'authenticated', user })
      return user
    },
    [api, setState],
  )
  const signOut = useCallback(async () => {
    await logoutIgnoringExpiredSession(() => api.logout())
    setState({ status: 'anonymous', reason: 'signedOut' })
  }, [api, setState])
  const expire = useCallback(() => setState({ status: 'anonymous', reason: 'expired' }), [setState])
  const recheck = useCallback(() => {
    setState({ status: 'checking' })
    restartCheck()
  }, [setState, restartCheck])
  return useMemo(() => ({ state, signIn, signOut, expire, recheck }), [state, signIn, signOut, expire, recheck])
}

// Um 401 no logout significa que a sessão já tinha caído: o resultado desejado já vale.
async function logoutIgnoringExpiredSession(logout: () => Promise<void>): Promise<void> {
  try {
    await logout()
  } catch (error) {
    if (!isSessionExpired(error)) {
      throw error
    }
  }
}

/**
 * Sessão atual e suas ações.
 *
 * Exemplo: `const { state, signOut } = useSession()`.
 */
// oxlint-disable-next-line react/only-export-components -- hook acompanha o provider do mesmo contexto
export function useSession(): SessionControls {
  const session = useContext(SessionContext)
  if (session === null) {
    throw new Error('useSession requires <SessionProvider>, got no provider above this component')
  }
  return session
}

/**
 * Usuário autenticado; só pode ser usado dentro de rotas protegidas por `RequireSession`.
 *
 * Exemplo: `const user = useCurrentUser(); user.role === 'ADMIN'`.
 */
// oxlint-disable-next-line react/only-export-components -- hook acompanha o provider do mesmo contexto
export function useCurrentUser(): CurrentUser {
  const { state } = useSession()
  if (state.status !== 'authenticated') {
    throw new Error(`useCurrentUser requires an authenticated session, got status '${state.status}'`)
  }
  return state.user
}
