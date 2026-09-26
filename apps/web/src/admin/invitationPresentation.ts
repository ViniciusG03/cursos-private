import type { InvitationDeliveryStatus, InvitationView } from '../api/types'

const DELIVERY_LABELS: Record<InvitationDeliveryStatus, string> = {
  SENT: 'Enviado',
  FAILED: 'Falha no envio',
  PENDING: 'Envio pendente',
}

const DATE_TIME = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })

/**
 * Rótulo em texto da situação do envio (não depende de cor).
 *
 * Exemplo: `deliveryLabel('FAILED') === 'Falha no envio'`.
 */
export function deliveryLabel(status: InvitationDeliveryStatus): string {
  return DELIVERY_LABELS[status]
}

/**
 * Validade do convite em texto, marcando "Expirado" quando já passou.
 *
 * Exemplo: `describeExpiration(invitation, Date.now())` → `'Expira em 29/09/2026, 10:00'`.
 */
export function describeExpiration(invitation: InvitationView, nowMillis: number): string {
  const expiresAt = new Date(invitation.expiresAt)
  const formatted = DATE_TIME.format(expiresAt)
  return expiresAt.getTime() <= nowMillis ? `Expirado em ${formatted}` : `Expira em ${formatted}`
}

/**
 * Mensagem após emitir ou reenviar, conforme o resultado do envio devolvido pela API.
 *
 * Exemplo: `describeIssueResult(view)` → `'Convite enviado para ana@x.com.'`.
 */
export function describeIssueResult(invitation: InvitationView): { tone: 'success' | 'error'; text: string } {
  if (invitation.deliveryStatus === 'FAILED') {
    return {
      tone: 'error',
      text: `O convite para ${invitation.email} foi registrado, mas o e-mail não pôde ser enviado. Tente reenviar.`,
    }
  }
  if (invitation.deliveryStatus === 'PENDING') {
    return { tone: 'success', text: `Convite para ${invitation.email} registrado; o envio está pendente.` }
  }
  return { tone: 'success', text: `Convite enviado para ${invitation.email}.` }
}
