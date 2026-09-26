import type { ReactNode } from 'react'

/**
 * Moldura das páginas públicas (login, convite, recuperação): sem navegação protegida e sem
 * recursos externos, já que algumas recebem token no link.
 *
 * Exemplo: `<PublicLayout title="Entrar">...</PublicLayout>`.
 */
export function PublicLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="public-shell">
      <header className="public-brand">
        <span className="brand-mark" aria-hidden="true" />
        Biblioteca de cursos
      </header>
      <main className="public-card" aria-labelledby="public-title">
        <h1 id="public-title">{title}</h1>
        {children}
      </main>
    </div>
  )
}
