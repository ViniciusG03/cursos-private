import { Link, Navigate, Outlet, useLocation } from 'react-router'
import type { LoginRedirectState } from '../access/LoginPage'
import { describeApiError } from '../ui/describeApiError'
import { LoadingState, Notice } from '../ui/Feedback'
import { homePathFor } from './postLoginPath'
import { useCurrentUser, useSession } from './SessionContext'

/**
 * Só renderiza as rotas filhas com sessão confirmada por /api/auth/me. Sem sessão, vai ao login
 * levando apenas o caminho interno atual (sem query nem fragmento).
 *
 * Exemplo: `<Route element={<RequireSession />}>...</Route>`.
 */
export function RequireSession() {
  const { state, recheck } = useSession()
  const location = useLocation()
  if (state.status === 'checking') {
    return <LoadingState label="Verificando sessão…" />
  }
  if (state.status === 'unavailable') {
    return <SessionUnavailable error={state.error} onRetry={recheck} />
  }
  if (state.status === 'anonymous') {
    // Quem saiu por vontade própria recomeça da página do papel; quem perdeu a sessão volta de onde estava.
    const redirect: LoginRedirectState = state.reason === 'signedOut' ? {} : { from: location.pathname }
    return <Navigate to="/entrar" replace state={redirect} />
  }
  return <Outlet />
}

function SessionUnavailable({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  return (
    <main className="standalone">
      <Notice tone="error">{describeApiError(error)}</Notice>
      <button type="button" className="button" onClick={onRetry}>
        Tentar novamente
      </button>
    </main>
  )
}

/**
 * Rotas administrativas: MEMBER vê acesso negado. A API continua sendo quem autoriza de fato.
 *
 * Exemplo: `<Route path="admin" element={<RequireAdmin />}>...</Route>`.
 */
export function RequireAdmin() {
  const user = useCurrentUser()
  if (user.role !== 'ADMIN') {
    return (
      <section aria-labelledby="denied-title">
        <h1 id="denied-title">Acesso negado</h1>
        <p>Esta área é exclusiva do administrador.</p>
        <Link to={homePathFor(user.role)}>Ir para os cursos</Link>
      </section>
    )
  }
  return <Outlet />
}

/**
 * Rota `/`: escolhe login, catálogo ou administração conforme a sessão.
 *
 * Exemplo: `<Route index element={<HomeRedirect />} />`.
 */
export function HomeRedirect() {
  const { state } = useSession()
  if (state.status === 'authenticated') {
    return <Navigate to={homePathFor(state.user.role)} replace />
  }
  if (state.status === 'anonymous') {
    return <Navigate to="/entrar" replace />
  }
  return <RequireSession />
}
