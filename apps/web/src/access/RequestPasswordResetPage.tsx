import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'
import { useLibraryApi } from '../api/LibraryApiContext'
import { usePendingAction } from '../data/usePendingAction'
import { describeApiError } from '../ui/describeApiError'
import { Notice } from '../ui/Feedback'
import { SubmitButton, TextField } from '../ui/FormControls'
import { PublicLayout } from '../ui/PublicLayout'

// A mesma confirmação para qualquer e-mail bem formado: a tela não revela se a conta existe.
const UNIFORM_CONFIRMATION =
  'Se houver uma conta para este e-mail, enviaremos um link de recuperação em instantes. O link vale por 30 minutos; só o mais recente funciona.'

const REQUEST_MESSAGES = {
  400: 'Informe um e-mail válido, como nome@exemplo.com.',
}

/**
 * Pedido de link de recuperação (POST /api/auth/password-resets/request, sempre 202).
 *
 * Exemplo: rota `/recuperar-acesso`.
 */
export function RequestPasswordResetPage() {
  const api = useLibraryApi()
  const request = usePendingAction({ expireSessionOn401: false })
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const outcome = await request.run(() => api.requestPasswordReset(email.trim()))
    setSent(outcome.ok)
  }

  return (
    <PublicLayout title="Recuperar acesso">
      {sent && <Notice tone="success">{UNIFORM_CONFIRMATION}</Notice>}
      {request.error !== null && <Notice tone="error">{describeApiError(request.error, REQUEST_MESSAGES)}</Notice>}
      <p>Informe o e-mail da sua conta para receber um link e definir uma nova senha.</p>
      <form onSubmit={handleSubmit}>
        <TextField label="E-mail" type="email" autoComplete="email" required value={email} onValueChange={setEmail} />
        <SubmitButton pending={request.pending} pendingLabel="Enviando…">
          Enviar link
        </SubmitButton>
      </form>
      <p className="form-footer">
        <Link to="/entrar">Voltar para o login</Link>
      </p>
    </PublicLayout>
  )
}
