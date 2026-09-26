import { createContext, useContext } from 'react'
import type { ReactNode } from 'react'
import type { CapturedLinkToken } from './linkToken'

const LinkTokenContext = createContext<CapturedLinkToken>({ pathname: '', token: null })

/**
 * Disponibiliza o token capturado em main.tsx para as páginas de convite e recuperação.
 *
 * Exemplo: `<LinkTokenProvider value={takeLinkToken(location, history)}>...</LinkTokenProvider>`.
 */
export function LinkTokenProvider({ value, children }: { value: CapturedLinkToken; children: ReactNode }) {
  return <LinkTokenContext.Provider value={value}>{children}</LinkTokenContext.Provider>
}

/**
 * Token do link se ele chegou por esta rota; senão null (link ausente ou de outra página).
 *
 * Exemplo: `const token = useLinkTokenFor(INVITATION_PATH)`.
 */
// oxlint-disable-next-line react/only-export-components -- hook acompanha o provider do mesmo contexto
export function useLinkTokenFor(pathname: string): string | null {
  const captured = useContext(LinkTokenContext)
  return captured.pathname === pathname ? captured.token : null
}
