import { useState } from 'react'
import { ApiError } from '../api/ApiError'
import { useLibraryApi } from '../api/LibraryApiContext'
import type { CourseOutline } from '../api/types'
import { usePendingAction } from '../data/usePendingAction'
import { describeApiError } from '../ui/describeApiError'
import { Notice } from '../ui/Feedback'
import { describePublishBlockers } from './catalogRules'

interface PublishPanelProps {
  outline: CourseOutline
  onChanged: (message: string | null) => void
}

/**
 * Publicação com confirmação explícita. Mostra as pendências estruturais antes e, num 409, o motivo com
 * os títulos dos módulos. Curso publicado não oferece despublicação (não há endpoint).
 *
 * Exemplo: `<PublishPanel outline={outline} onChanged={announceAndReload} />`.
 */
export function PublishPanel({ outline, onChanged }: PublishPanelProps) {
  if (outline.status === 'PUBLISHED') {
    return (
      <section className="panel" aria-labelledby="publish-title">
        <h2 id="publish-title">Publicação</h2>
        <p>
          Curso publicado: membros já podem vê-lo. Ainda é possível acrescentar aulas e reordenar; novos módulos não.
        </p>
      </section>
    )
  }
  return <DraftPublishPanel outline={outline} onChanged={onChanged} />
}

function DraftPublishPanel({ outline, onChanged }: PublishPanelProps) {
  const api = useLibraryApi()
  const publish = usePendingAction({ expireSessionOn401: true })
  const [confirming, setConfirming] = useState(false)
  const blockers = describePublishBlockers(outline)

  const confirm = async () => {
    const outcome = await publish.run(() => api.publishCourse(outline.id))
    setConfirming(false)
    // Recarrega também na falha: num 409 a estrutura pode ter mudado em outra aba.
    onChanged(outcome.ok ? 'Curso publicado. Membros convidados já podem vê-lo.' : null)
  }

  return (
    <section className="panel" aria-labelledby="publish-title">
      <h2 id="publish-title">Publicação</h2>
      <p>Rascunho: só o administrador vê este curso. A publicação é estrutural; os vídeos entram depois.</p>
      <PublishBlockers blockers={blockers} />
      <PublishError error={publish.error} blockers={blockers} />
      {confirming ? (
        <div className="confirm-box" role="group" aria-labelledby="confirm-publish-text">
          <p id="confirm-publish-text">Publicar "{outline.title}"? Membros passarão a ver o curso e ele não poderá ganhar novos módulos.</p>
          <button type="button" className="button" disabled={publish.pending} onClick={confirm}>
            {publish.pending ? 'Publicando…' : 'Confirmar publicação'}
          </button>{' '}
          <button type="button" className="button button-secondary" disabled={publish.pending} onClick={() => setConfirming(false)}>
            Cancelar
          </button>
        </div>
      ) : (
        <button type="button" className="button" onClick={() => setConfirming(true)}>
          Publicar curso…
        </button>
      )}
    </section>
  )
}

function PublishBlockers({ blockers }: { blockers: string[] }) {
  if (blockers.length === 0) return null
  return (
    <div className="blockers">
      <p>Antes de publicar:</p>
      <ul>
        {blockers.map((blocker) => (
          <li key={blocker}>{blocker}</li>
        ))}
      </ul>
    </div>
  )
}

function PublishError({ error, blockers }: { error: unknown; blockers: string[] }) {
  if (error === null) return null
  const incomplete = error instanceof ApiError && error.status === 409
  const detail = blockers.length > 0 ? blockers.join(' ') : 'A estrutura do curso mudou; confira a versão recarregada.'
  const message = incomplete ? `O curso ainda não pode ser publicado. ${detail}` : describeApiError(error)
  return <Notice tone="error">{message}</Notice>
}
