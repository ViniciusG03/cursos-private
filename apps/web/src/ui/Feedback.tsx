import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { describeApiError } from './describeApiError'
import type { StatusMessages } from './describeApiError'

/**
 * Indicador de carregamento anunciado por leitores de tela.
 *
 * Exemplo: `<LoadingState label="Carregando cursos…" />`.
 */
export function LoadingState({ label }: { label: string }) {
  return (
    <p className="loading" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      {label}
    </p>
  )
}

interface NoticeProps {
  tone: 'success' | 'info' | 'error'
  children: ReactNode
  // false quando o aviso já está dentro de uma região viva permanente (evita anúncio duplicado).
  live?: boolean
}

/**
 * Aviso de resultado: `success`/`info` são educados (role=status), `error` interrompe (role=alert).
 *
 * Exemplo: `<Notice tone="success">Convite enviado.</Notice>`.
 */
export function Notice({ tone, children, live = true }: NoticeProps) {
  const role = !live ? undefined : tone === 'error' ? 'alert' : 'status'
  return (
    <div className={`notice notice-${tone}`} role={role}>
      <span className="notice-label">{NOTICE_LABELS[tone]}</span> {children}
    </div>
  )
}

// Rótulo textual para não depender só da cor.
const NOTICE_LABELS = { success: 'Pronto:', info: 'Aviso:', error: 'Erro:' } as const

/**
 * Falha de carregamento com botão explícito de nova tentativa.
 *
 * Exemplo: `<ErrorState error={error} onRetry={reload} />`.
 */
export function ErrorState(props: { error: unknown; onRetry: () => void; messages?: StatusMessages }) {
  return (
    <div className="error-state">
      <Notice tone="error">{describeApiError(props.error, props.messages)}</Notice>
      <button type="button" className="button button-secondary" onClick={props.onRetry}>
        Tentar novamente
      </button>
    </div>
  )
}

/**
 * Estado vazio com título e orientação.
 *
 * Exemplo: `<EmptyState title="Nenhum curso publicado">Volte mais tarde.</EmptyState>`.
 */
export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="empty-state">
      <p className="empty-title">{title}</p>
      {children && <p>{children}</p>}
    </div>
  )
}

/**
 * Conteúdo de "não encontrado", usado pela rota 404 e por recursos que a API responde 404.
 *
 * Exemplo: `<NotFoundContent backTo="/cursos" backLabel="Voltar aos cursos" />`.
 */
export function NotFoundContent({ backTo, backLabel }: { backTo: string; backLabel: string }) {
  return (
    <section className="not-found" aria-labelledby="not-found-title">
      <h1 id="not-found-title">Página não encontrada</h1>
      <p>O endereço não existe ou o conteúdo não está disponível para você.</p>
      <Link to={backTo}>{backLabel}</Link>
    </section>
  )
}
