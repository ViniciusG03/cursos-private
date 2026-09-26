import { useId } from 'react'
import type { InputHTMLAttributes, ReactNode } from 'react'

type NativeInputProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'id' | 'onChange' | 'value'>

interface TextFieldProps extends NativeInputProps {
  label: string
  value: string
  onValueChange: (value: string) => void
  error?: string | null
  hint?: ReactNode
  multiline?: boolean
}

/**
 * Campo com rótulo, dica e erro associados por `aria-describedby`.
 *
 * Exemplo: `<TextField label="E-mail" type="email" value={email} onValueChange={setEmail} />`.
 */
export function TextField({ label, value, onValueChange, error, hint, multiline, ...native }: TextFieldProps) {
  const id = useId()
  const hintId = hint ? `${id}-hint` : undefined
  const errorId = error ? `${id}-error` : undefined
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined
  const shared = { id, value, 'aria-invalid': error ? true : undefined, 'aria-describedby': describedBy }
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {hint && <p className="field-hint" id={hintId}>{hint}</p>}
      {multiline ? (
        <textarea {...shared} name={native.name} rows={3} onChange={(event) => onValueChange(event.target.value)} />
      ) : (
        <input {...native} {...shared} onChange={(event) => onValueChange(event.target.value)} />
      )}
      {error && <p className="field-error" id={errorId}>{error}</p>}
    </div>
  )
}

/**
 * Botão de envio que fica desabilitado e anuncia o estado enquanto a operação está pendente.
 *
 * Exemplo: `<SubmitButton pending={login.pending} pendingLabel="Entrando…">Entrar</SubmitButton>`.
 */
export function SubmitButton(props: { pending: boolean; pendingLabel: string; children: ReactNode }) {
  return (
    <button type="submit" className="button" disabled={props.pending} aria-busy={props.pending}>
      {props.pending ? props.pendingLabel : props.children}
    </button>
  )
}
