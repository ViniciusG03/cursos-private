import { useState } from 'react'
import type { FormEvent } from 'react'
import { SubmitButton, TextField } from '../ui/FormControls'
import { PASSWORD_MAX_CHARACTERS, PASSWORD_MIN_CHARACTERS, validateNewPassword } from './passwordRules'
import type { PasswordFormErrors } from './passwordRules'

interface NewPasswordFormProps {
  submitLabel: string
  pending: boolean
  onSubmit: (password: string) => void
}

const NO_ERRORS: PasswordFormErrors = { password: null, confirmation: null }

/**
 * Senha e confirmação com a política da API validada antes do envio. A senha fica só no estado do
 * componente; nunca vai para URL, storage ou log.
 *
 * Exemplo: `<NewPasswordForm submitLabel="Definir senha" pending={false} onSubmit={send} />`.
 */
export function NewPasswordForm({ submitLabel, pending, onSubmit }: NewPasswordFormProps) {
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [errors, setErrors] = useState<PasswordFormErrors>(NO_ERRORS)

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const found = validateNewPassword(password, confirmation)
    setErrors(found)
    if (found.password === null && found.confirmation === null) onSubmit(password)
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <TextField
        label="Nova senha"
        type="password"
        autoComplete="new-password"
        value={password}
        onValueChange={setPassword}
        error={errors.password}
        hint={`Use de ${PASSWORD_MIN_CHARACTERS} a ${PASSWORD_MAX_CHARACTERS} caracteres. Uma frase longa é mais fácil de lembrar.`}
      />
      <TextField
        label="Confirme a nova senha"
        type="password"
        autoComplete="new-password"
        value={confirmation}
        onValueChange={setConfirmation}
        error={errors.confirmation}
      />
      <SubmitButton pending={pending} pendingLabel="Salvando…">
        {submitLabel}
      </SubmitButton>
    </form>
  )
}
