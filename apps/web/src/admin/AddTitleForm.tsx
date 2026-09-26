import { useState } from 'react'
import type { FormEvent } from 'react'
import type { PendingAction } from '../data/usePendingAction'
import { describeApiError } from '../ui/describeApiError'
import type { StatusMessages } from '../ui/describeApiError'
import { Notice } from '../ui/Feedback'
import { SubmitButton, TextField } from '../ui/FormControls'
import { validateCatalogTitle } from './catalogRules'

interface AddTitleFormProps {
  label: string
  submitLabel: string
  maxLength: number
  action: PendingAction
  errorMessages: StatusMessages
  onSubmit: (title: string) => Promise<boolean>
}

/**
 * Formulário de um campo de título para acrescentar módulo ou aula. Limpa o campo após sucesso.
 *
 * Exemplo: `<AddTitleForm label="Título do módulo" submitLabel="Adicionar módulo" ... />`.
 */
export function AddTitleForm({ label, submitLabel, maxLength, action, errorMessages, onSubmit }: AddTitleFormProps) {
  const [title, setTitle] = useState('')
  const [validationError, setValidationError] = useState<string | null>(null)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const problem = validateCatalogTitle(title, maxLength)
    setValidationError(problem)
    if (problem === null && (await onSubmit(title.trim()))) setTitle('')
  }

  return (
    <form className="inline-form" onSubmit={handleSubmit} noValidate>
      <TextField label={label} value={title} onValueChange={setTitle} error={validationError} maxLength={maxLength} />
      <SubmitButton pending={action.pending} pendingLabel="Adicionando…">
        {submitLabel}
      </SubmitButton>
      {action.error !== null && <Notice tone="error">{describeApiError(action.error, errorMessages)}</Notice>}
    </form>
  )
}
