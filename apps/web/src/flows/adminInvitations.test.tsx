import { screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { FakeLibraryApi, fakeInvitation, httpError } from '../test/FakeLibraryApi'
import { ADMIN_EMAIL, MEMBER_EMAIL, PASSWORD, renderLibraryApp } from '../test/renderLibraryApp'

let api: FakeLibraryApi

beforeEach(async () => {
  api = new FakeLibraryApi()
  api.addAccount('ADMIN', ADMIN_EMAIL, PASSWORD)
  api.addAccount('MEMBER', MEMBER_EMAIL, PASSWORD)
  await api.login(ADMIN_EMAIL, PASSWORD)
})

function rowFor(email: string): HTMLElement {
  return screen.getByRole('cell', { name: email }).closest('tr')!
}

describe('admin invitations', () => {
  it('lists delivery status and validity in text, including expired ones', async () => {
    const expired = { ...fakeInvitation('i2', 'velho@exemplo.com', 'FAILED'), expiresAt: '2020-01-01T00:00:00Z' }
    api.invitations.push(fakeInvitation('i1', 'ana@exemplo.com', 'SENT'), expired)
    renderLibraryApp(api, '/admin/convites')

    await screen.findByRole('cell', { name: 'ana@exemplo.com' })
    expect(within(rowFor('ana@exemplo.com')).getByText('Enviado')).toBeInTheDocument()
    expect(within(rowFor('ana@exemplo.com')).getByText(/^Expira em/)).toBeInTheDocument()
    expect(within(rowFor('velho@exemplo.com')).getByText('Falha no envio')).toBeInTheDocument()
    expect(within(rowFor('velho@exemplo.com')).getByText(/^Expirado em/)).toBeInTheDocument()
  })

  it('issues an invitation and refreshes the list', async () => {
    const { user } = renderLibraryApp(api, '/admin/convites')
    await user.type(await screen.findByLabelText('E-mail'), 'nova@exemplo.com')

    await user.click(screen.getByRole('button', { name: 'Enviar convite' }))

    expect(await screen.findByText('Convite enviado para nova@exemplo.com.')).toBeInTheDocument()
    expect(await screen.findByRole('cell', { name: 'nova@exemplo.com' })).toBeInTheDocument()
    expect(screen.getByLabelText('E-mail')).toHaveValue('')
  })

  it('warns when delivery failed', async () => {
    api.deliveryOutcome = 'FAILED'
    const { user } = renderLibraryApp(api, '/admin/convites')
    await user.type(await screen.findByLabelText('E-mail'), 'falha@exemplo.com')

    await user.click(screen.getByRole('button', { name: 'Enviar convite' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('o e-mail não pôde ser enviado. Tente reenviar.')
  })

  it('explains 409 for an e-mail that already has an account', async () => {
    const { user } = renderLibraryApp(api, '/admin/convites')
    await user.type(await screen.findByLabelText('E-mail'), MEMBER_EMAIL)

    await user.click(screen.getByRole('button', { name: 'Enviar convite' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Este e-mail já tem conta')
  })

  it('resends with the same e-mail only after confirming the old link stops working', async () => {
    api.invitations.push(fakeInvitation('i1', 'falhou@exemplo.com', 'FAILED'))
    const { user } = renderLibraryApp(api, '/admin/convites')

    await user.click(await screen.findByRole('button', { name: 'Reenviar convite para falhou@exemplo.com' }))
    expect(screen.getByText('O link enviado antes deixará de funcionar.')).toBeInTheDocument()
    expect(api.callsTo('issueInvitation')).toHaveLength(0)
    await user.click(screen.getByRole('button', { name: 'Confirmar reenvio' }))

    expect(await screen.findByText('Convite enviado para falhou@exemplo.com.')).toBeInTheDocument()
    expect(api.callsTo('issueInvitation')[0].args).toEqual(['falhou@exemplo.com'])
    expect(await within(rowFor('falhou@exemplo.com')).findByText('Enviado')).toBeInTheDocument()
  })

  it('handles 429 from the API with the wait time', async () => {
    api.failNext('issueInvitation', httpError(429, 'too many', 5))
    const { user } = renderLibraryApp(api, '/admin/convites')
    await user.type(await screen.findByLabelText('E-mail'), 'nova@exemplo.com')

    await user.click(screen.getByRole('button', { name: 'Enviar convite' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('5 segundos')
  })
})
