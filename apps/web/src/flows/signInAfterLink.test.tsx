import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { ApiError } from '../api/ApiError'
import { FakeLibraryApi } from '../test/FakeLibraryApi'
import { ADMIN_EMAIL, MEMBER_EMAIL, PASSWORD, renderLibraryApp } from '../test/renderLibraryApp'

// Regressão: com outra conta conectada, "Ir para o login" caía em /entrar, que redireciona usuários
// autenticados, e a conta recém-criada (ou a senha recém-trocada) não tinha como entrar.

const INVITE_TOKEN = 'convite-token'
const RESET_TOKEN = 'reset-token'
const INVITED_EMAIL = 'convidada@exemplo.com'
const NEW_PASSWORD = 'nova frase longa e segura'

let api: FakeLibraryApi

type AppUser = ReturnType<typeof renderLibraryApp>['user']

beforeEach(() => {
  api = new FakeLibraryApi()
  api.addAccount('ADMIN', ADMIN_EMAIL, PASSWORD)
  api.addAccount('MEMBER', MEMBER_EMAIL, PASSWORD)
  api.validInvitationTokens.set(INVITE_TOKEN, INVITED_EMAIL)
  api.validResetTokens.set(RESET_TOKEN, MEMBER_EMAIL)
})

async function submitNewPassword(user: AppUser, buttonName: string) {
  await user.type(await screen.findByLabelText('Nova senha'), NEW_PASSWORD)
  await user.type(screen.getByLabelText('Confirme a nova senha'), NEW_PASSWORD)
  await user.click(screen.getByRole('button', { name: buttonName }))
}

async function signInThroughForm(user: AppUser, email: string, password: string) {
  expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
  expect(window.location.pathname).toBe('/entrar')
  await user.type(screen.getByLabelText('E-mail'), email)
  await user.type(screen.getByLabelText('Senha'), password)
  await user.click(screen.getByRole('button', { name: 'Entrar' }))
}

describe('invitation accepted while another account is signed in', () => {
  it('keeps the old session until the explicit switch, then reaches the login form', async () => {
    await api.login(ADMIN_EMAIL, PASSWORD)
    const { user } = renderLibraryApp(api, `/convites/aceitar#token=${INVITE_TOKEN}`)
    expect(window.location.hash).toBe('')

    await submitNewPassword(user, 'Criar conta')

    expect(await screen.findByText(/ainda está conectado como admin@exemplo.com/)).toBeInTheDocument()
    expect(api.callsTo('logout')).toHaveLength(0)
    expect(api.callsTo('login')).toHaveLength(1)
    await user.click(screen.getByRole('button', { name: 'Sair e entrar com outra conta' }))
    await signInThroughForm(user, INVITED_EMAIL, NEW_PASSWORD)

    expect(await screen.findByRole('heading', { name: 'Cursos' })).toBeInTheDocument()
    expect(screen.getByText(INVITED_EMAIL)).toBeInTheDocument()
    expect(api.callsTo('logout')).toHaveLength(1)
  })

  it('lets the person keep using the current account instead', async () => {
    await api.login(ADMIN_EMAIL, PASSWORD)
    const { user } = renderLibraryApp(api, `/convites/aceitar#token=${INVITE_TOKEN}`)
    await submitNewPassword(user, 'Criar conta')

    await user.click(await screen.findByRole('link', { name: `Continuar como ${ADMIN_EMAIL}` }))

    expect(await screen.findByRole('heading', { name: 'Organizar cursos' })).toBeInTheDocument()
    expect(api.callsTo('logout')).toHaveLength(0)
  })

  it('shows the logout failure and stays on the page', async () => {
    await api.login(ADMIN_EMAIL, PASSWORD)
    api.failNext('logout', ApiError.network())
    const { user } = renderLibraryApp(api, `/convites/aceitar#token=${INVITE_TOKEN}`)
    await submitNewPassword(user, 'Criar conta')

    await user.click(await screen.findByRole('button', { name: 'Sair e entrar com outra conta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível falar com o servidor')
    expect(window.location.pathname).toBe('/convites/aceitar')
  })

  it('keeps the no-session path: straight link to the login form, no session created', async () => {
    const { user } = renderLibraryApp(api, `/convites/aceitar#token=${INVITE_TOKEN}`)
    await submitNewPassword(user, 'Criar conta')

    await user.click(await screen.findByRole('link', { name: 'Ir para o login' }))
    await signInThroughForm(user, INVITED_EMAIL, NEW_PASSWORD)

    expect(await screen.findByRole('heading', { name: 'Cursos' })).toBeInTheDocument()
    expect(api.callsTo('login')).toHaveLength(1)
  })
})

describe('password reset while signed in', () => {
  it('rechecks the session of the same account, which the API ended, and opens the login form directly', async () => {
    await api.login(MEMBER_EMAIL, PASSWORD)
    const { user } = renderLibraryApp(api, `/recuperar-acesso/nova-senha#token=${RESET_TOKEN}`)

    await submitNewPassword(user, 'Salvar nova senha')
    await user.click(await screen.findByRole('link', { name: 'Ir para o login' }))
    await signInThroughForm(user, MEMBER_EMAIL, NEW_PASSWORD)

    expect(await screen.findByRole('heading', { name: 'Cursos' })).toBeInTheDocument()
    // Nenhuma página protegida foi aberta antes do novo login para descobrir o 401.
    const firstCatalogCall = api.calls.findIndex((call) => call.operation === 'listCourses')
    const secondLogin = api.calls.map((call) => call.operation).lastIndexOf('login')
    expect(firstCatalogCall).toBeGreaterThan(secondLogin)
    expect(screen.queryByText(/Sua sessão expirou/)).not.toBeInTheDocument()
  })

  it('offers the explicit switch when a different account is signed in', async () => {
    await api.login(ADMIN_EMAIL, PASSWORD)
    const { user } = renderLibraryApp(api, `/recuperar-acesso/nova-senha#token=${RESET_TOKEN}`)
    await submitNewPassword(user, 'Salvar nova senha')

    await user.click(await screen.findByRole('button', { name: 'Sair e entrar com outra conta' }))
    await signInThroughForm(user, MEMBER_EMAIL, NEW_PASSWORD)

    expect(await screen.findByRole('heading', { name: 'Cursos' })).toBeInTheDocument()
    expect(screen.getByText(MEMBER_EMAIL)).toBeInTheDocument()
  })
})
