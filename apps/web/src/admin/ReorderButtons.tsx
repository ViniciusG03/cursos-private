import { useEffect, useRef } from 'react'
import type { MoveDirection } from './catalogRules'

interface ReorderButtonsProps {
  itemLabel: string
  index: number
  count: number
  disabled: boolean
  focusRequested: MoveDirection | null
  onMove: (direction: MoveDirection) => void
  onFocusHandled: () => void
}

/**
 * Botões "Subir"/"Descer" acessíveis por teclado. Depois de mover, o foco volta ao botão usado na
 * nova posição (ou ao outro, se o item chegou à ponta), para seguir movendo pelo teclado. O pai só
 * passa `focusRequested` quando o item já está na posição esperada.
 *
 * Exemplo: `<ReorderButtons itemLabel="módulo Introdução" index={0} count={3} ... />`.
 */
export function ReorderButtons(props: ReorderButtonsProps) {
  const { itemLabel, index, count, disabled, focusRequested, onMove, onFocusHandled } = props
  const upRef = useRef<HTMLButtonElement>(null)
  const downRef = useRef<HTMLButtonElement>(null)
  const canMoveUp = index > 0
  const canMoveDown = index < count - 1

  useEffect(() => {
    if (focusRequested === null || disabled) return
    const preferUp = focusRequested === 'up' ? canMoveUp : !canMoveDown
    ;(preferUp ? upRef : downRef).current?.focus()
    onFocusHandled()
  }, [focusRequested, disabled, canMoveUp, canMoveDown, onFocusHandled])

  return (
    <span className="reorder-buttons">
      <button type="button" ref={upRef} className="button button-small button-secondary" disabled={disabled || !canMoveUp}
        aria-label={`Subir ${itemLabel}`} onClick={() => onMove('up')}>
        ↑ Subir
      </button>
      <button type="button" ref={downRef} className="button button-small button-secondary" disabled={disabled || !canMoveDown}
        aria-label={`Descer ${itemLabel}`} onClick={() => onMove('down')}>
        ↓ Descer
      </button>
    </span>
  )
}
