import { Link, useNavigate } from 'react-router'
import { usePendingAction } from '../data/usePendingAction'
import { useSession } from '../session/SessionContext'
import { describeApiError } from '../ui/describeApiError'
import { LoadingState, Notice } from '../ui/Feedback'

/**
 * Caminho até o formulário de login depois de definir uma senha por link. Com outra conta ainda
 * conectada neste navegador, /entrar redirecionaria de volta à área dela; por isso a troca de conta é
 * uma ação explícita (logout e só então login). Abrir o link não encerra a sessão antiga.
 *
 * Exemplo: `<SignInNextStep />` logo após o aviso de sucesso.
 */
export function SignInNextStep() {
  const { state } = useSession()
  if (state.status === 'checking') {
    return <LoadingState label="Verificando sessão…" />
  }
  if (state.status === 'authenticated') {
    return <SwitchAccount email={state.user.email} />
  }
  return (
    <p>
      <Link className="button" to="/entrar">
        Ir para o login
      </Link>
    </p>
  )
}

function SwitchAccount({ email }: { email: string }) {
  const { signOut } = useSession()
  const navigate = useNavigate()
  const logout = usePendingAction({ expireSessionOn401: false })
  const switchAccount = async () => {
    const outcome = await logout.run(signOut)
    if (outcome.ok) navigate('/entrar')
  }
  return (
    <>
      <Notice tone="info">
        Este navegador ainda está conectado como {email}. Para entrar com a outra conta, saia desta primeiro.
      </Notice>
      {logout.error !== null && <Notice tone="error">{describeApiError(logout.error)}</Notice>}
      <p className="inline-actions">
        <button type="button" className="button" disabled={logout.pending} onClick={switchAccount}>
          {logout.pending ? 'Saindo…' : 'Sair e entrar com outra conta'}
        </button>
        <Link to="/">Continuar como {email}</Link>
      </p>
    </>
  )
}
