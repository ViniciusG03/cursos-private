import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { usePendingAction } from '../data/usePendingAction'
import { resolvePostLoginPath } from '../session/postLoginPath'
import { useSession } from '../session/SessionContext'
import type { AnonymousReason } from '../session/SessionContext'
import { describeApiError } from '../ui/describeApiError'
import { LoadingState, Notice } from '../ui/Feedback'
import { SubmitButton, TextField } from '../ui/FormControls'
import { PublicLayout } from '../ui/PublicLayout'

/** Estado de navegação recebido do guarda de rotas: a rota interna que pediu login. */
export interface LoginRedirectState {
  from?: string
}

const LOGIN_MESSAGES = {
  // Mesma mensagem para e-mail inexistente e senha errada, como a API.
  401: 'E-mail ou senha incorretos.',
  403: 'Não foi possível validar a proteção do formulário. Tente entrar novamente.',
}

/**
 * Login por e-mail e senha. Depois do login, volta à rota interna permitida ou à página do papel.
 *
 * Exemplo: rota `/entrar`.
 */
export function LoginPage() {
  const { state } = useSession()
  const location = useLocation()
  const from = (location.state as LoginRedirectState | null)?.from
  if (state.status === 'checking') {
    return <LoadingState label="Verificando sessão…" />
  }
  if (state.status === 'authenticated') {
    return <Navigate to={resolvePostLoginPath(from, state.user.role)} replace />
  }
  const reason = state.status === 'anonymous' ? state.reason : 'initial'
  return (
    <PublicLayout title="Entrar">
      <SessionEndedNotice reason={reason} />
      <LoginForm />
      <p className="form-footer">
        <Link to="/recuperar-acesso">Esqueci minha senha</Link>
      </p>
    </PublicLayout>
  )
}

function SessionEndedNotice({ reason }: { reason: AnonymousReason }) {
  if (reason === 'expired') return <Notice tone="info">Sua sessão expirou. Entre novamente para continuar.</Notice>
  if (reason === 'signedOut') return <Notice tone="success">Você saiu da biblioteca.</Notice>
  return null
}

// Após o login a sessão fica autenticada e o <Navigate> de LoginPage leva ao destino.
function LoginForm() {
  const { signIn } = useSession()
  const login = usePendingAction({ expireSessionOn401: false })
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await login.run(() => signIn(email.trim(), password))
  }

  return (
    <form onSubmit={handleSubmit}>
      {login.error !== null && <Notice tone="error">{describeApiError(login.error, LOGIN_MESSAGES)}</Notice>}
      <TextField label="E-mail" type="email" autoComplete="username" required value={email} onValueChange={setEmail} />
      <TextField
        label="Senha"
        type="password"
        autoComplete="current-password"
        required
        value={password}
        onValueChange={setPassword}
      />
      <SubmitButton pending={login.pending} pendingLabel="Entrando…">
        Entrar
      </SubmitButton>
    </form>
  )
}
