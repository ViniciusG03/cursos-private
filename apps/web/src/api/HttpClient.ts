import { ApiError } from './ApiError'
import type { CsrfToken } from './types'

/** Assinatura do `fetch` injetada no cliente; os testes passam uma implementação fake. */
export type FetchFunction = (input: string, init: RequestInit) => Promise<Response>

/** Corpo de uma escrita: JSON, formulário urlencoded (login) ou nenhum. */
export type WritePayload = { json: unknown } | { form: URLSearchParams } | null

type WriteMethod = 'POST' | 'PUT'

const CSRF_PATH = '/api/auth/csrf'

/**
 * Cliente HTTP da SPA: URLs relativas (mesma origem), cookies de sessão, CSRF novo a cada escrita e
 * conversão de respostas não-2xx em {@link ApiError}. Nunca guarda token, senha ou sessão.
 *
 * Exemplo: `const courses = await client.getJson<CourseSummary[]>('/api/courses')`.
 */
export class HttpClient {
  private readonly fetchFunction: FetchFunction

  constructor(fetchFunction: FetchFunction) {
    this.fetchFunction = fetchFunction
  }

  /**
   * GET com corpo JSON.
   *
   * Exemplo: `await client.getJson<CurrentUser>('/api/auth/me')`.
   */
  async getJson<T>(path: string): Promise<T> {
    const response = await this.execute(path, { method: 'GET', headers: { Accept: 'application/json' } })
    return (await response.json()) as T
  }

  /**
   * Escrita que devolve JSON (ex.: 201 `{id}`).
   *
   * Exemplo: `await client.writeJson<CreatedResource>('POST', '/api/admin/courses', { json: course })`.
   */
  async writeJson<T>(method: WriteMethod, path: string, payload: WritePayload): Promise<T> {
    const response = await this.write(method, path, payload)
    return (await response.json()) as T
  }

  /**
   * Escrita sem corpo de resposta (202/204): não tenta ler JSON.
   *
   * Exemplo: `await client.writeWithoutBody('POST', '/api/auth/logout', null)`.
   */
  async writeWithoutBody(method: WriteMethod, path: string, payload: WritePayload): Promise<void> {
    await this.write(method, path, payload)
  }

  // O token CSRF muda no login e no logout; buscá-lo antes de cada escrita evita reusar um vencido.
  private async write(method: WriteMethod, path: string, payload: WritePayload): Promise<Response> {
    const csrf = await this.getJson<CsrfToken>(CSRF_PATH)
    const headers: Record<string, string> = { Accept: 'application/json', [csrf.headerName]: csrf.token }
    return this.execute(path, { method, headers, body: encodePayload(payload, headers) })
  }

  private async execute(path: string, init: RequestInit): Promise<Response> {
    let response: Response
    try {
      response = await this.fetchFunction(path, { ...init, credentials: 'same-origin' })
    } catch {
      throw ApiError.network()
    }
    if (!response.ok) {
      throw await ApiError.fromResponse(response)
    }
    return response
  }
}

function encodePayload(payload: WritePayload, headers: Record<string, string>): BodyInit | undefined {
  if (payload === null) {
    return undefined
  }
  if ('form' in payload) {
    headers['Content-Type'] = 'application/x-www-form-urlencoded'
    return payload.form.toString()
  }
  headers['Content-Type'] = 'application/json'
  return JSON.stringify(payload.json)
}
