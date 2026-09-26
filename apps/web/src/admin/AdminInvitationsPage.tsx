import { useState } from 'react'
import type { FormEvent } from 'react'
import type { LibraryApi } from '../api/LibraryApi'
import { useLibraryApi } from '../api/LibraryApiContext'
import { useApiResource } from '../data/useApiResource'
import { usePendingAction } from '../data/usePendingAction'
import type { PendingAction } from '../data/usePendingAction'
import { describeApiError } from '../ui/describeApiError'
import { Notice } from '../ui/Feedback'
import { SubmitButton, TextField } from '../ui/FormControls'
import { ResourceView } from '../ui/ResourceView'
import { describeIssueResult } from './invitationPresentation'
import { InvitationTable } from './InvitationTable'

// O instante da leitura decide o "Expirado" na tabela; medi-lo aqui mantém a renderização pura.
const loadInvitations = async (api: LibraryApi) => ({
  invitations: await api.listInvitations(),
  loadedAtMillis: Date.now(),
})

const ISSUE_MESSAGES = {
  400: 'Informe um e-mail válido, como nome@exemplo.com.',
  409: 'Este e-mail já tem conta (ou outro envio para ele está em andamento). Não é preciso convidar de novo.',
}

type IssueResult = { tone: 'success' | 'error'; text: string } | null

/**
 * Emissão, listagem e reenvio de convites. Reenviar é um novo POST com o mesmo e-mail e revoga o link
 * anterior; o token nunca aparece aqui.
 *
 * Exemplo: rota `/admin/convites`.
 */
export function AdminInvitationsPage() {
  const api = useLibraryApi()
  const { state, reload } = useApiResource(loadInvitations)
  const issue = usePendingAction({ expireSessionOn401: true })
  const [result, setResult] = useState<IssueResult>(null)

  const send = async (email: string) => {
    const outcome = await issue.run(() => api.issueInvitation(email))
    setResult(outcome.ok ? describeIssueResult(outcome.value) : null)
    reload()
    return outcome.ok
  }

  return (
    <section aria-labelledby="invitations-title">
      <h1 id="invitations-title">Convites</h1>
      <div role="status" className="live-region">
        {result && result.tone === 'success' && <Notice tone="success" live={false}>{result.text}</Notice>}
      </div>
      {result && result.tone === 'error' && <Notice tone="error">{result.text}</Notice>}
      {issue.error !== null && <Notice tone="error">{describeApiError(issue.error, ISSUE_MESSAGES)}</Notice>}
      <IssueInvitationForm action={issue} onSend={send} />
      <h2>Convites em aberto</h2>
      <p className="muted">Reenviar gera um link novo e invalida o anterior. Convites valem 72 horas.</p>
      <ResourceView state={state} loadingLabel="Carregando convites…" onRetry={reload} notFoundBackTo="/admin/cursos">
        {(loaded) => (
          <InvitationTable
            invitations={loaded.invitations}
            nowMillis={loaded.loadedAtMillis}
            pending={issue.pending}
            onResend={send}
          />
        )}
      </ResourceView>
    </section>
  )
}

function IssueInvitationForm({ action, onSend }: { action: PendingAction; onSend: (email: string) => Promise<boolean> }) {
  const [email, setEmail] = useState('')
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (await onSend(email.trim())) setEmail('')
  }
  return (
    <form className="panel inline-form" onSubmit={handleSubmit} aria-labelledby="issue-title">
      <h2 id="issue-title">Convidar pessoa</h2>
      <TextField label="E-mail" type="email" autoComplete="off" required value={email} onValueChange={setEmail} />
      <SubmitButton pending={action.pending} pendingLabel="Enviando…">
        Enviar convite
      </SubmitButton>
    </form>
  )
}
