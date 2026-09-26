import { useState } from 'react'
import type { InvitationView } from '../api/types'
import { EmptyState } from '../ui/Feedback'
import { deliveryLabel, describeExpiration } from './invitationPresentation'

interface InvitationTableProps {
  invitations: InvitationView[]
  pending: boolean
  onResend: (email: string) => Promise<boolean>
  nowMillis: number
}

/**
 * Convites em aberto com situação do envio, validade e reenvio confirmado.
 *
 * Exemplo: `<InvitationTable invitations={list} pending={false} onResend={send} />`.
 */
export function InvitationTable({ invitations, pending, onResend, nowMillis }: InvitationTableProps) {
  const [confirmingEmail, setConfirmingEmail] = useState<string | null>(null)
  if (invitations.length === 0) {
    return <EmptyState title="Nenhum convite em aberto" />
  }
  const resend = async (email: string) => {
    await onResend(email)
    setConfirmingEmail(null)
  }
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th scope="col">E-mail</th>
            <th scope="col">Envio</th>
            <th scope="col">Validade</th>
            <th scope="col">Ações</th>
          </tr>
        </thead>
        <tbody>
          {invitations.map((invitation) => (
            <tr key={invitation.id}>
              <td>{invitation.email}</td>
              <td>
                <span className={`badge badge-delivery-${invitation.deliveryStatus.toLowerCase()}`}>
                  {deliveryLabel(invitation.deliveryStatus)}
                </span>
              </td>
              <td>{describeExpiration(invitation, nowMillis)}</td>
              <td>
                <ResendControl
                  email={invitation.email}
                  confirming={confirmingEmail === invitation.email}
                  pending={pending}
                  onAsk={() => setConfirmingEmail(invitation.email)}
                  onCancel={() => setConfirmingEmail(null)}
                  onConfirm={() => resend(invitation.email)}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

interface ResendControlProps {
  email: string
  confirming: boolean
  pending: boolean
  onAsk: () => void
  onCancel: () => void
  onConfirm: () => void
}

function ResendControl({ email, confirming, pending, onAsk, onCancel, onConfirm }: ResendControlProps) {
  if (!confirming) {
    return (
      <button type="button" className="button button-small button-secondary" disabled={pending} onClick={onAsk}
        aria-label={`Reenviar convite para ${email}`}>
        Reenviar
      </button>
    )
  }
  return (
    <div className="confirm-box" role="group" aria-label={`Confirmar reenvio para ${email}`}>
      <p>O link enviado antes deixará de funcionar.</p>
      <button type="button" className="button button-small" disabled={pending} onClick={onConfirm}>
        {pending ? 'Reenviando…' : 'Confirmar reenvio'}
      </button>{' '}
      <button type="button" className="button button-small button-secondary" disabled={pending} onClick={onCancel}>
        Cancelar
      </button>
    </div>
  )
}
