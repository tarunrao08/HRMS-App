import { useCallback, useState } from "react"

/**
 * Wraps a dialog's real close action behind a "Discard changes?" confirmation.
 * Call `requestClose(isDirty)` from the Dialog's onOpenChange (or a Cancel
 * button) instead of closing directly; render a <ConfirmDialog> wired to the
 * returned props for the confirmation itself.
 */
export function useConfirmClose(onClose: () => void) {
  const [confirmOpen, setConfirmOpen] = useState(false)

  const requestClose = useCallback((isDirty: boolean) => {
    if (isDirty) {
      setConfirmOpen(true)
    } else {
      onClose()
    }
  }, [onClose])

  const confirmDiscard = useCallback(() => {
    setConfirmOpen(false)
    onClose()
  }, [onClose])

  return { confirmOpen, setConfirmOpen, requestClose, confirmDiscard }
}
