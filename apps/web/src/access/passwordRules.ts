// Espelha PassphrasePolicy da API: 12 a 128 code points, sem aparar espaços. A API decide no final.
export const PASSWORD_MIN_CHARACTERS = 12
export const PASSWORD_MAX_CHARACTERS = 128

/** Erros por campo do formulário de nova senha; null quando o campo está válido. */
export interface PasswordFormErrors {
  password: string | null
  confirmation: string | null
}

/**
 * Conta caracteres como a API (code points): um emoji ou acento conta como um.
 *
 * Exemplo: `countPasswordCharacters('😀') === 1`.
 */
export function countPasswordCharacters(password: string): number {
  return Array.from(password).length
}

/**
 * Valida senha e confirmação antes do envio.
 *
 * Exemplo: `validateNewPassword('curta', 'curta').password !== null`.
 */
export function validateNewPassword(password: string, confirmation: string): PasswordFormErrors {
  return {
    password: describePasswordProblem(password),
    confirmation: password === confirmation ? null : 'A confirmação precisa ser igual à senha.',
  }
}

function describePasswordProblem(password: string): string | null {
  const characters = countPasswordCharacters(password)
  if (password.trim() === '') {
    return `Informe uma senha de ${PASSWORD_MIN_CHARACTERS} a ${PASSWORD_MAX_CHARACTERS} caracteres.`
  }
  if (characters < PASSWORD_MIN_CHARACTERS || characters > PASSWORD_MAX_CHARACTERS) {
    return `A senha precisa ter de ${PASSWORD_MIN_CHARACTERS} a ${PASSWORD_MAX_CHARACTERS} caracteres; tem ${characters}.`
  }
  return null
}
