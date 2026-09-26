import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FakeLibraryApi, httpError } from '../test/FakeLibraryApi'
import { PASSWORD, renderLibraryApp } from '../test/renderLibraryApp'

const INVITE_TOKEN = 'convite-token-secreto'
const RESET_TOKEN = 'reset-token-secreto'

let api: FakeLibraryApi

beforeEach(() => {
  api = new FakeLibraryApi()
  api.validInvitationTokens.set(INVITE_TOKEN, 'nova@exemplo.com')
  api.addAccount('MEMBER', 'membro@exemplo.com', 'senha antiga do membro')
  api.validResetTokens.set(RESET_TOKEN, 'membro@exemplo.com')
})

type AppUser = ReturnType<typeof renderLibraryApp>['user']

async function choosePassword(user: AppUser, password: string, confirmation = password) {
  await user.type(await screen.findByLabelText('Nova senha'), password)
  await user.type(screen.getByLabelText('Confirme a nova senha'), confirmation)
}

function expectNoSecretPersisted(...secrets: string[]) {
  const persisted = JSON.stringify({ ...window.localStorage }) + JSON.stringify({ ...window.sessionStorage })
  for (const secret of secrets) {
    expect(persisted).not.toContain(secret)
    expect(window.location.href).not.toContain(secret)
    expect(document.body.textContent).not.toContain(secret)
  }
}

describe('invitation acceptance', () => {
  it('removes #token from the address bar before any API call', async () => {
    renderLibraryApp(api, `/convites/aceitar#token=${INVITE_TOKEN}`)

    await screen.findByLabelText('Nova senha')

    expect(window.location.hash).toBe('')
    expect(api.calls.length).toBeGreaterThan(0)
    expect(api.calls.every((call) => call.hashAtCall === '')).toBe(true)
  })

  it('sends token and password only in the accept body and guides to login without a session', async () => {
    const logSpy = vi.spyOn(console, 'log')
    const { user } = renderLibraryApp(api, `/convites/aceitar#token=${INVITE_TOKEN}`)
    await choosePassword(user, PASSWORD)

    await user.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByText(/Conta criada/)).toBeInTheDocument()
    expect(api.callsTo('acceptInvitation')[0].args).toEqual([{ token: INVITE_TOKEN, password: PASSWORD }])
    expect(api.callsTo('login')).toHaveLength(0)
    await expect(api.currentUser()).rejects.toMatchObject({ status: 401 })
    expect(screen.getByRole('link', { name: 'Ir para o login' })).toHaveAttribute('href', '/entrar')
    expectNoSecretPersisted(INVITE_TOKEN, PASSWORD)
    expect(logSpy).not.toHaveBeenCalled()
  })

  it('validates the password on the client without calling the API', async () => {
    const { user } = renderLibraryApp(api, `/convites/aceitar#token=${INVITE_TOKEN}`)
    await choosePassword(user, 'curta', 'outra')

    await user.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(screen.getByLabelText('Nova senha')).toHaveAccessibleDescription(/tem 5/)
    expect(screen.getByLabelText('Confirme a nova senha')).toHaveAttribute('aria-invalid', 'true')
    expect(api.callsTo('acceptInvitation')).toHaveLength(0)
  })

  it('shows a safe state for an expired, revoked or used link without echoing the token', async () => {
    api.validInvitationTokens.clear()
    const { user } = renderLibraryApp(api, `/convites/aceitar#token=${INVITE_TOKEN}`)
    await choosePassword(user, PASSWORD)

    await user.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Peça um novo convite')
    expect(screen.queryByLabelText('Nova senha')).not.toBeInTheDocument()
    expectNoSecretPersisted(INVITE_TOKEN)
  })

  it('shows the missing-link state when the URL has no token', async () => {
    renderLibraryApp(api, '/convites/aceitar')

    expect(await screen.findByRole('alert')).toHaveTextContent('link de convite é inválido')
    expect(screen.queryByLabelText('Nova senha')).not.toBeInTheDocument()
  })

  it('explains 409 when the account already exists', async () => {
    api.failNext('acceptInvitation', httpError(409, 'account exists'))
    const { user } = renderLibraryApp(api, `/convites/aceitar#token=${INVITE_TOKEN}`)
    await choosePassword(user, PASSWORD)

    await user.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Já existe uma conta')
  })

  it('keeps the form for a retry after 429', async () => {
    api.failNext('acceptInvitation', httpError(429, 'too many', 15))
    const { user } = renderLibraryApp(api, `/convites/aceitar#token=${INVITE_TOKEN}`)
    await choosePassword(user, PASSWORD)
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('15 segundos')

    await user.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByText(/Conta criada/)).toBeInTheDocument()
  })
})

describe('password recovery', () => {
  it('shows the same confirmation whether or not the account exists', async () => {
    const { user } = renderLibraryApp(api, '/recuperar-acesso')
    await user.type(await screen.findByLabelText('E-mail'), 'ninguem@exemplo.com')

    await user.click(screen.getByRole('button', { name: 'Enviar link' }))

    expect(await screen.findByText(/Se houver uma conta para este e-mail/)).toBeInTheDocument()
    expect(api.callsTo('requestPasswordReset')[0].args).toEqual(['ninguem@exemplo.com'])
  })

  // O navegador já barra "sem-arroba" (type=email); "nome@dominio" passa por ele e a API responde 400.
  it('reports an e-mail the API considers malformed (400)', async () => {
    api.failNext('requestPasswordReset', httpError(400, 'email must be like name@example.com'))
    const { user } = renderLibraryApp(api, '/recuperar-acesso')
    await user.type(await screen.findByLabelText('E-mail'), 'nome@dominio')

    await user.click(screen.getByRole('button', { name: 'Enviar link' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Informe um e-mail válido')
  })

  it('clears #token and confirms the new password in the request body', async () => {
    const { user } = renderLibraryApp(api, `/recuperar-acesso/nova-senha#token=${RESET_TOKEN}`)
    expect(window.location.hash).toBe('')
    await choosePassword(user, PASSWORD)

    await user.click(screen.getByRole('button', { name: 'Salvar nova senha' }))

    expect(await screen.findByText(/Senha alterada/)).toBeInTheDocument()
    expect(api.callsTo('confirmPasswordReset')[0].args).toEqual([{ token: RESET_TOKEN, password: PASSWORD }])
    expect(api.calls.every((call) => call.hashAtCall === '')).toBe(true)
    expectNoSecretPersisted(RESET_TOKEN, PASSWORD)
  })

  it('offers a new link when the reset token was rejected', async () => {
    api.validResetTokens.clear()
    const { user } = renderLibraryApp(api, `/recuperar-acesso/nova-senha#token=${RESET_TOKEN}`)
    await choosePassword(user, PASSWORD)

    await user.click(screen.getByRole('button', { name: 'Salvar nova senha' }))

    expect(await screen.findByRole('link', { name: 'Pedir um novo link' })).toHaveAttribute('href', '/recuperar-acesso')
  })

  it('does not reuse a token captured on a different route', async () => {
    renderLibraryApp(api, `/convites/aceitar#token=${INVITE_TOKEN}`)
    await screen.findByLabelText('Nova senha')
    window.history.pushState(null, '', '/recuperar-acesso/nova-senha')
    window.dispatchEvent(new PopStateEvent('popstate'))

    expect(await screen.findByRole('alert')).toHaveTextContent('link de recuperação é inválido')
  })
})
