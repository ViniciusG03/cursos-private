import { useState } from 'react'
import type { ReactNode } from 'react'
import { ApiError } from '../api/ApiError'
import type { LibraryApi } from '../api/LibraryApi'
import { useLibraryApi } from '../api/LibraryApiContext'
import type { TokenPassword } from '../api/types'
import { usePendingAction } from '../data/usePendingAction'
import { useSession } from '../session/SessionContext'
import { describeApiError } from '../ui/describeApiError'
import type { StatusMessages } from '../ui/describeApiError'
import { Notice } from '../ui/Feedback'
import { PublicLayout } from '../ui/PublicLayout'
import { useLinkTokenFor } from './LinkTokenContext'
import { NewPasswordForm } from './NewPasswordForm'
import { SignInNextStep } from './SignInNextStep'

/** Textos e operação que diferenciam o aceite de convite da confirmação de recuperação. */
export interface TokenPasswordFlow {
  title: string
  intro: string
  tokenPath: string
  submitLabel: string
  successMessage: string
  invalidLink: ReactNode
  errorMessages: StatusMessages
  send: (api: LibraryApi, request: TokenPassword) => Promise<void>
}

type Stage = 'form' | 'invalidLink' | 'done'

/**
 * Página de definição de senha a partir de um link com token. O token veio do fragmento (já apagado
 * da URL) e só sai no corpo do POST. Após 204 orienta o login: nenhuma sessão é criada aqui.
 *
 * Exemplo: `<TokenPasswordPage flow={INVITATION_FLOW} />`.
 */
export function TokenPasswordPage({ flow }: { flow: TokenPasswordFlow }) {
  const api = useLibraryApi()
  const token = useLinkTokenFor(flow.tokenPath)
  const [stage, setStage] = useState<Stage>(token === null ? 'invalidLink' : 'form')
  const action = usePendingAction({ expireSessionOn401: false })
  const { recheck } = useSession()

  const submit = async (password: string) => {
    if (token === null) return
    const outcome = await action.run(() => flow.send(api, { token, password }))
    if (!outcome.ok) return
    setStage('done')
    // A recuperação derruba as sessões da conta; reconsultar /me evita que o login redirecione para
    // uma área protegida só para então descobrir o 401. Sessão de outra conta continua valendo.
    recheck()
  }
  // Com a política de senha validada no cliente, um 400 aqui significa link inválido, expirado ou usado.
  const linkRejected = action.error instanceof ApiError && action.error.status === 400

  return (
    <PublicLayout title={flow.title}>
      {stage === 'done' && <DoneMessage message={flow.successMessage} />}
      {(stage === 'invalidLink' || linkRejected) && <Notice tone="error">{flow.invalidLink}</Notice>}
      {stage === 'form' && !linkRejected && (
        <>
          <p>{flow.intro}</p>
          {action.error !== null && <Notice tone="error">{describeApiError(action.error, flow.errorMessages)}</Notice>}
          <NewPasswordForm submitLabel={flow.submitLabel} pending={action.pending} onSubmit={submit} />
        </>
      )}
    </PublicLayout>
  )
}

function DoneMessage({ message }: { message: string }) {
  return (
    <>
      <Notice tone="success">{message}</Notice>
      <SignInNextStep />
    </>
  )
}
