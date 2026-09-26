/** Origem da falha: resposta HTTP de erro ou requisição que nem chegou a ter resposta. */
export type ApiErrorKind = 'http' | 'network'

interface ProblemBody {
  detail?: unknown
}

/**
 * Falha de uma chamada à API, preservando status, `detail` do problem+json e `Retry-After` para que
 * cada tela decida a mensagem. A mensagem cita só método e caminho: nunca corpo, senha ou token.
 *
 * Exemplo: `if (error instanceof ApiError && error.status === 404) showNotFound()`.
 */
export class ApiError extends Error {
  readonly kind: ApiErrorKind
  readonly status: number
  readonly detail: string | null
  readonly retryAfterSeconds: number | null

  constructor(kind: ApiErrorKind, status: number, detail: string | null, retryAfterSeconds: number | null) {
    super(kind === 'network' ? 'network failure' : `HTTP ${status}${detail ? `: ${detail}` : ''}`)
    this.name = 'ApiError'
    this.kind = kind
    this.status = status
    this.detail = detail
    this.retryAfterSeconds = retryAfterSeconds
  }

  /**
   * Erro para falha de rede (fetch rejeitado); status 0.
   *
   * Exemplo: `throw ApiError.network()`.
   */
  static network(): ApiError {
    return new ApiError('network', 0, null, null)
  }

  /**
   * Lê status, `detail` e `Retry-After` de uma resposta não-2xx.
   *
   * Exemplo: `if (!response.ok) throw await ApiError.fromResponse(response)`.
   */
  static async fromResponse(response: Response): Promise<ApiError> {
    const detail = await readProblemDetail(response)
    return new ApiError('http', response.status, detail, parseRetryAfter(response.headers.get('Retry-After')))
  }
}

async function readProblemDetail(response: Response): Promise<string | null> {
  const contentType = response.headers.get('Content-Type') ?? ''
  if (!contentType.includes('json')) {
    return null
  }
  try {
    const problem = (await response.json()) as ProblemBody
    return typeof problem.detail === 'string' ? problem.detail : null
  } catch {
    return null
  }
}

/**
 * Converte o cabeçalho `Retry-After` (segundos) em número; ausente ou inválido vira null.
 *
 * Exemplo: `parseRetryAfter('30') === 30`.
 */
export function parseRetryAfter(headerValue: string | null): number | null {
  if (headerValue === null || !/^\d+$/.test(headerValue.trim())) {
    return null
  }
  return Number(headerValue.trim())
}

/**
 * Indica se o erro é um 401 da API (sessão ausente ou expirada).
 *
 * Exemplo: `if (isSessionExpired(error)) session.expire()`.
 */
export function isSessionExpired(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401
}
