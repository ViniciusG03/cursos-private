import { NavLink, Outlet } from 'react-router'
import { usePendingAction } from '../data/usePendingAction'
import { useCurrentUser, useSession } from '../session/SessionContext'
import { describeApiError } from '../ui/describeApiError'
import { Notice } from '../ui/Feedback'

/**
 * Moldura das áreas autenticadas: navegação por papel, usuário atual e saída.
 *
 * Exemplo: `<Route element={<AppShell />}>...</Route>`.
 */
export function AppShell() {
  const user = useCurrentUser()
  return (
    <div className="app-shell">
      <a className="skip-link" href="#conteudo">
        Pular para o conteúdo
      </a>
      <header className="app-header">
        <NavLink to="/" className="brand">
          <span className="brand-mark" aria-hidden="true" />
          Biblioteca de cursos
        </NavLink>
        <nav aria-label="Principal" className="main-nav">
          <NavLink to="/cursos" end>
            Cursos
          </NavLink>
          {user.role === 'ADMIN' && <NavLink to="/admin/cursos">Organizar cursos</NavLink>}
          {user.role === 'ADMIN' && <NavLink to="/admin/convites">Convites</NavLink>}
        </nav>
        <SignOutControl email={user.email} />
      </header>
      <main id="conteudo" className="app-content" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  )
}

function SignOutControl({ email }: { email: string }) {
  const { signOut } = useSession()
  const logout = usePendingAction({ expireSessionOn401: false })
  return (
    <div className="account">
      <span className="account-email">{email}</span>
      <button type="button" className="button button-secondary" disabled={logout.pending} onClick={() => logout.run(signOut)}>
        {logout.pending ? 'Saindo…' : 'Sair'}
      </button>
      {logout.error !== null && <Notice tone="error">{describeApiError(logout.error)}</Notice>}
    </div>
  )
}
