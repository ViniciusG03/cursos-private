/** Rotas cujos links de e-mail trazem `#token=...` (docs/acesso-api.md, "Links enviados por e-mail"). */
export const INVITATION_PATH = '/convites/aceitar'
export const PASSWORD_RESET_CONFIRM_PATH = '/recuperar-acesso/nova-senha'

const TOKEN_PATHS = [INVITATION_PATH, PASSWORD_RESET_CONFIRM_PATH]

/** Token lido do link, associado à rota em que chegou; vive só na memória da página. */
export interface CapturedLinkToken {
  pathname: string
  token: string | null
}

/**
 * Lê `#token=` nas rotas de convite/recuperação e apaga o fragmento da barra com `replaceState`
 * antes de qualquer renderização ou requisição. Precisa rodar fora do React: o StrictMode executa
 * inicializadores duas vezes e a segunda leitura não encontraria mais o fragmento.
 *
 * Exemplo: `const linkToken = takeLinkToken(window.location, window.history)`.
 */
export function takeLinkToken(location: Location, history: History): CapturedLinkToken {
  const pathname = location.pathname
  if (!TOKEN_PATHS.includes(pathname) || location.hash === '') {
    return { pathname, token: null }
  }
  const token = new URLSearchParams(location.hash.slice(1)).get('token')
  history.replaceState(history.state, '', pathname + location.search)
  return { pathname, token: token && token.trim() !== '' ? token : null }
}

/**
 * Colar um link novo na aba que já está numa rota de token muda só o fragmento: não há carga nova e
 * {@link takeLinkToken} não rodaria. Recarregar faz o link passar pela captura inicial, que limpa a URL.
 *
 * Exemplo: `watchLinkTokenHashChanges(window, () => window.location.reload())`.
 */
export function watchLinkTokenHashChanges(target: Window, reloadDocument: () => void): () => void {
  const onHashChange = () => {
    const { pathname, hash } = target.location
    if (TOKEN_PATHS.includes(pathname) && new URLSearchParams(hash.slice(1)).has('token')) reloadDocument()
  }
  target.addEventListener('hashchange', onHashChange)
  return () => target.removeEventListener('hashchange', onHashChange)
}
