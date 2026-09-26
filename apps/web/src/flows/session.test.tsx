import { screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { ApiError } from '../api/ApiError'
import { FakeLibraryApi, httpError } from '../test/FakeLibraryApi'
import { ADMIN_EMAIL, MEMBER_EMAIL, PASSWORD, publishedCourseFixture, renderLibraryApp } from '../test/renderLibraryApp'

let api: FakeLibraryApi

beforeEach(() => {
  api = new FakeLibraryApi()
  api.addAccount('ADMIN', ADMIN_EMAIL, PASSWORD)
  api.addAccount('MEMBER', MEMBER_EMAIL, PASSWORD)
})

async function signIn(user: ReturnType<typeof renderLibraryApp>['user'], email: string, password = PASSWORD) {
  await user.type(await screen.findByLabelText('E-mail'), email)
  await user.type(screen.getByLabelText('Senha'), password)
  await user.click(screen.getByRole('button', { name: 'Entrar' }))
}

describe('session bootstrap', () => {
  it('shows no protected content while /me is pending', async () => {
    const release = api.hold('currentUser')
    renderLibraryApp(api, '/cursos')

    expect(screen.getByRole('status')).toHaveTextContent('Verificando sessão')
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument()
    release()
    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
  })

  it('offers a retry when /me fails for network reasons', async () => {
    api.failNext('currentUser', ApiError.network())
    const { user } = renderLibraryApp(api, '/cursos')

    await user.click(await screen.findByRole('button', { name: 'Tentar novamente' }))

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
  })

  it('sends / to the login page when there is no session', async () => {
    renderLibraryApp(api, '/')

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
    expect(window.location.pathname).toBe('/entrar')
  })
})

describe('login', () => {
  it('takes ADMIN to /admin/cursos and MEMBER to /cursos', async () => {
    const admin = renderLibraryApp(api, '/entrar')
    await signIn(admin.user, ADMIN_EMAIL)
    expect(await screen.findByRole('heading', { name: 'Organizar cursos' })).toBeInTheDocument()
    expect(window.location.pathname).toBe('/admin/cursos')
    admin.unmount()

    api.dropSession()
    const member = renderLibraryApp(api, '/entrar')
    await signIn(member.user, MEMBER_EMAIL)
    expect(await screen.findByRole('heading', { name: 'Cursos' })).toBeInTheDocument()
    expect(window.location.pathname).toBe('/cursos')
    expect(screen.queryByRole('link', { name: 'Organizar cursos' })).not.toBeInTheDocument()
  })

  it('shows a generic error for wrong credentials', async () => {
    const { user } = renderLibraryApp(api, '/entrar')

    await signIn(user, ADMIN_EMAIL, 'senha errada qualquer')

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha incorretos.')
  })

  it('shows the Retry-After wait on 429', async () => {
    api.failNext('login', httpError(429, 'too many attempts', 120))
    const { user } = renderLibraryApp(api, '/entrar')

    await signIn(user, ADMIN_EMAIL)

    expect(await screen.findByRole('alert')).toHaveTextContent('Aguarde 2 minutos')
  })

  it('explains a 403 (stale CSRF) without retrying on its own', async () => {
    api.failNext('login', httpError(403, 'access denied'))
    const { user } = renderLibraryApp(api, '/entrar')

    await signIn(user, ADMIN_EMAIL)

    expect(await screen.findByRole('alert')).toHaveTextContent('proteção do formulário')
    expect(api.callsTo('login')).toHaveLength(1)
  })

  it('blocks double submission while login is pending', async () => {
    const release = api.hold('login')
    const { user } = renderLibraryApp(api, '/entrar')
    await user.type(await screen.findByLabelText('E-mail'), ADMIN_EMAIL)
    await user.type(screen.getByLabelText('Senha'), PASSWORD)

    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    await user.click(screen.getByRole('button', { name: 'Entrando…' }))
    release()

    await screen.findByRole('heading', { name: 'Organizar cursos' })
    expect(api.callsTo('login')).toHaveLength(1)
  })

  it('returns to the protected route that required login', async () => {
    const course = api.addCourse(publishedCourseFixture())
    const { user } = renderLibraryApp(api, `/cursos/${course.id}`)

    await signIn(user, MEMBER_EMAIL)

    expect(await screen.findByRole('heading', { name: 'Java moderno' })).toBeInTheDocument()
    expect(window.location.pathname).toBe(`/cursos/${course.id}`)
  })
})

describe('authorization and session end', () => {
  it('denies /admin to MEMBER', async () => {
    await api.login(MEMBER_EMAIL, PASSWORD)
    renderLibraryApp(api, '/admin/cursos')

    expect(await screen.findByRole('heading', { name: 'Acesso negado' })).toBeInTheDocument()
    expect(api.callsTo('listCourses')).toHaveLength(0)
  })

  it('logs out, hides protected navigation and cannot go back into it', async () => {
    await api.login(ADMIN_EMAIL, PASSWORD)
    const { user } = renderLibraryApp(api, '/cursos')

    await user.click(await screen.findByRole('button', { name: 'Sair' }))

    expect(await screen.findByText(/Você saiu/)).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Principal' })).not.toBeInTheDocument()
    await expect(api.currentUser()).rejects.toMatchObject({ status: 401 })
  })

  it('treats 401 on logout as already signed out', async () => {
    await api.login(ADMIN_EMAIL, PASSWORD)
    const { user } = renderLibraryApp(api, '/cursos')
    await screen.findByRole('button', { name: 'Sair' })
    api.dropSession()

    await user.click(screen.getByRole('button', { name: 'Sair' }))

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
  })

  it('sends an expired session to login and back to the same route afterwards', async () => {
    const course = api.addCourse(publishedCourseFixture())
    await api.login(MEMBER_EMAIL, PASSWORD)
    const { user } = renderLibraryApp(api, '/cursos')
    await screen.findByRole('link', { name: 'Java moderno' })
    api.dropSession()

    await user.click(screen.getByRole('link', { name: 'Java moderno' }))

    expect(await screen.findByText(/Sua sessão expirou/)).toBeInTheDocument()
    await signIn(user, MEMBER_EMAIL)
    await waitFor(() => expect(window.location.pathname).toBe(`/cursos/${course.id}`))
  })

  it('shows a 404 page for unknown routes', async () => {
    renderLibraryApp(api, '/nao-existe')

    expect(await screen.findByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument()
  })
})
