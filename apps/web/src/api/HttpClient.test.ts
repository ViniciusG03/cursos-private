import { describe, expect, it } from 'vitest'
import { ApiError } from './ApiError'
import { HttpClient } from './HttpClient'
import { HttpLibraryApi } from './LibraryApi'

interface SentRequest {
  path: string
  init: RequestInit
}

/** `fetch` fake: devolve respostas enfileiradas por caminho e registra cada requisição. */
class FakeFetch {
  readonly sent: SentRequest[] = []
  private readonly responses = new Map<string, Array<() => Response>>()
  private csrfCounter = 0

  respond(path: string, factory: () => Response): void {
    this.responses.set(path, [...(this.responses.get(path) ?? []), factory])
  }

  readonly fetch = async (path: string, init: RequestInit): Promise<Response> => {
    this.sent.push({ path, init })
    if (path === '/api/auth/csrf') return this.csrfResponse()
    const factory = this.responses.get(path)?.shift()
    if (factory === undefined) throw new TypeError('Failed to fetch')
    return factory()
  }

  private csrfResponse(): Response {
    this.csrfCounter += 1
    return json(200, { headerName: 'X-CSRF-TOKEN', parameterName: '_csrf', token: `csrf-${this.csrfCounter}` })
  }
}

function json(status: number, body: unknown, headers: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json', ...headers } })
}

function problem(status: number, detail: string, headers: Record<string, string> = {}): Response {
  const body = { type: 'about:blank', title: 'x', status, detail }
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/problem+json', ...headers } })
}

function createApi() {
  const fakeFetch = new FakeFetch()
  return { fakeFetch, api: new HttpLibraryApi(new HttpClient(fakeFetch.fetch)) }
}

describe('HttpClient', () => {
  it('fetches a fresh CSRF token before every write and sends it in the header named by the API', async () => {
    const { fakeFetch, api } = createApi()
    fakeFetch.respond('/api/admin/courses', () => json(201, { id: 'c1' }))
    fakeFetch.respond('/api/admin/courses/c1/publish', () => new Response(null, { status: 204 }))

    await api.createCourse({ title: 'Java', description: null })
    await api.publishCourse('c1')

    const writes = fakeFetch.sent.filter((request) => request.init.method !== 'GET')
    expect(writes.map((request) => new Headers(request.init.headers).get('X-CSRF-TOKEN'))).toEqual(['csrf-1', 'csrf-2'])
    expect(fakeFetch.sent.map((request) => request.path)).toEqual([
      '/api/auth/csrf',
      '/api/admin/courses',
      '/api/auth/csrf',
      '/api/admin/courses/c1/publish',
    ])
  })

  it('sends login as form-urlencoded with same-origin credentials', async () => {
    const { fakeFetch, api } = createApi()
    fakeFetch.respond('/api/auth/login', () => json(200, { id: 'u1', email: 'a@x.com', role: 'ADMIN' }))

    const user = await api.login('a@x.com', 'senha & com símbolos')

    const login = fakeFetch.sent.find((request) => request.path === '/api/auth/login')!
    expect(new Headers(login.init.headers).get('Content-Type')).toBe('application/x-www-form-urlencoded')
    expect(new URLSearchParams(String(login.init.body)).get('password')).toBe('senha & com símbolos')
    expect(login.init.credentials).toBe('same-origin')
    expect(user.role).toBe('ADMIN')
  })

  it('accepts 204 and 202 without reading a body', async () => {
    const { fakeFetch, api } = createApi()
    fakeFetch.respond('/api/auth/logout', () => new Response(null, { status: 204 }))
    fakeFetch.respond('/api/auth/password-resets/request', () => new Response(null, { status: 202 }))

    await expect(api.logout()).resolves.toBeUndefined()
    await expect(api.requestPasswordReset('a@x.com')).resolves.toBeUndefined()
  })

  it('keeps status and problem detail of error responses', async () => {
    const { fakeFetch, api } = createApi()
    fakeFetch.respond('/api/admin/courses/c1/publish', () => problem(409, 'course c1 cannot be published'))

    const error = await api.publishCourse('c1').catch((failure: unknown) => failure)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ kind: 'http', status: 409, detail: 'course c1 cannot be published' })
  })

  it('reads Retry-After from 429 responses', async () => {
    const { fakeFetch, api } = createApi()
    fakeFetch.respond('/api/auth/login', () => problem(429, 'too many attempts', { 'Retry-After': '42' }))

    await expect(api.login('a@x.com', 'x')).rejects.toMatchObject({ status: 429, retryAfterSeconds: 42 })
  })

  it('turns a rejected fetch into a network ApiError with status 0', async () => {
    const { api } = createApi()

    await expect(api.listCourses()).rejects.toMatchObject({ kind: 'network', status: 0 })
  })

  it('encodes path segments so an ID from the URL cannot change the endpoint', async () => {
    const { fakeFetch, api } = createApi()
    fakeFetch.respond('/api/courses/..%2Fadmin', () => problem(400, "parameter 'courseId' must be a valid UUID"))

    await expect(api.readCourse('../admin')).rejects.toMatchObject({ status: 400 })
    expect(fakeFetch.sent[0].path).toBe('/api/courses/..%2Fadmin')
  })

  it('never puts request bodies in the error message', async () => {
    const { fakeFetch, api } = createApi()
    fakeFetch.respond('/api/auth/invitations/accept', () => problem(400, 'invalid token'))

    const error = (await api.acceptInvitation({ token: 'segredo-token', password: 'segredo-senha' }).catch((f: unknown) => f)) as Error

    expect(error.message).not.toContain('segredo')
  })
})
