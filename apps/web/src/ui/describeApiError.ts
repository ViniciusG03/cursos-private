import { ApiError } from '../api/ApiError'

/** Mensagens específicas de uma tela por status HTTP; o que faltar usa o texto padrão. */
export type StatusMessages = Partial<Record<number, string>>

const NETWORK_MESSAGE = 'Não foi possível falar com o servidor. Verifique sua conexão e tente novamente.'

const DEFAULT_MESSAGES: Record<number, string> = {
  400: 'Os dados enviados não foram aceitos. Revise os campos e tente novamente.',
  401: 'Sua sessão expirou. Entre novamente para continuar.',
  // A API usa o mesmo 403 para papel sem permissão e CSRF vencido: não dá para repetir às cegas.
  403: 'A operação foi recusada: sem permissão ou proteção do formulário vencida. Atualize a página e tente de novo.',
  404: 'O item procurado não existe ou não está disponível para você.',
  409: 'A operação conflita com o estado atual. Atualize a página e confira os dados.',
}

/**
 * Converte qualquer falha de chamada em texto em português para a interface. Nunca inclui o corpo da
 * requisição (que pode ter senha ou token).
 *
 * Exemplo: `describeApiError(error, { 401: 'E-mail ou senha incorretos.' })`.
 */
export function describeApiError(error: unknown, messages: StatusMessages = {}): string {
  if (!(error instanceof ApiError)) {
    return 'Ocorreu um erro inesperado. Tente novamente.'
  }
  if (error.kind === 'network') {
    return NETWORK_MESSAGE
  }
  if (error.status === 429) {
    return messages[429] ?? describeTooManyAttempts(error.retryAfterSeconds)
  }
  const message = messages[error.status] ?? DEFAULT_MESSAGES[error.status]
  return message ?? 'O servidor encontrou um erro. Tente novamente em instantes.'
}

/**
 * Texto do 429, citando a espera do `Retry-After` quando existe.
 *
 * Exemplo: `describeTooManyAttempts(90) === 'Muitas tentativas. Aguarde 2 minutos e tente novamente.'`.
 */
export function describeTooManyAttempts(retryAfterSeconds: number | null): string {
  if (retryAfterSeconds === null) {
    return 'Muitas tentativas. Aguarde alguns minutos e tente novamente.'
  }
  if (retryAfterSeconds < 60) {
    return `Muitas tentativas. Aguarde ${retryAfterSeconds} segundos e tente novamente.`
  }
  const minutes = Math.ceil(retryAfterSeconds / 60)
  return `Muitas tentativas. Aguarde ${minutes} ${minutes === 1 ? 'minuto' : 'minutos'} e tente novamente.`
}

/**
 * Indica um 404 da API, ou um 400 de ID malformado na URL, que para o usuário também é "não existe".
 *
 * Exemplo: `if (isMissingResource(error)) return <NotFoundContent />`.
 */
export function isMissingResource(error: unknown): boolean {
  return error instanceof ApiError && (error.status === 404 || error.status === 400)
}
