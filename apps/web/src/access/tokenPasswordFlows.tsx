import { Link } from 'react-router'
import { INVITATION_PATH, PASSWORD_RESET_CONFIRM_PATH } from './linkToken'
import type { TokenPasswordFlow } from './TokenPasswordPage'

/** Aceite de convite: cria a conta MEMBER (POST /api/auth/invitations/accept). */
export const INVITATION_FLOW: TokenPasswordFlow = {
  title: 'Aceitar convite',
  intro: 'Defina a senha da sua conta. Depois, entre com seu e-mail e essa senha.',
  tokenPath: INVITATION_PATH,
  submitLabel: 'Criar conta',
  successMessage: 'Conta criada. Agora entre com seu e-mail e a senha que você definiu.',
  invalidLink: (
    <>
      Este link de convite é inválido, expirou, foi substituído por um reenvio ou já foi usado. Peça um novo convite ao
      administrador.
    </>
  ),
  errorMessages: {
    409: 'Já existe uma conta para este e-mail. Entre ou recupere o acesso.',
  },
  send: (api, request) => api.acceptInvitation(request),
}

/** Confirmação de recuperação: troca a senha (POST /api/auth/password-resets/confirm). */
export const PASSWORD_RESET_FLOW: TokenPasswordFlow = {
  title: 'Definir nova senha',
  intro: 'Escolha uma nova senha. Sessões abertas com a senha antiga serão encerradas.',
  tokenPath: PASSWORD_RESET_CONFIRM_PATH,
  submitLabel: 'Salvar nova senha',
  successMessage: 'Senha alterada. Entre novamente com a nova senha.',
  invalidLink: (
    <>
      Este link de recuperação é inválido, expirou ou já foi usado. Vale só o link do e-mail mais recente.{' '}
      <Link to="/recuperar-acesso">Pedir um novo link</Link>.
    </>
  ),
  errorMessages: {},
  send: (api, request) => api.confirmPasswordReset(request),
}
