import { useEffect, useRef } from "react"

/**
 * Snapshots `values` at the moment `open` flips from false to true, and exposes
 * `isDirty()` to compare the current `values` against that snapshot. Used to
 * gate a dialog's close behind a "Discard changes?" confirmation.
 */
export function useFormSnapshot<T>(open: boolean, values: T) {
  const snapshotRef = useRef<T>(values)
  const wasOpen = useRef(false)

  useEffect(() => {
    if (open && !wasOpen.current) {
      snapshotRef.current = values
    }
    wasOpen.current = open
    // Intentionally only re-snapshotting on the open transition, not on every
    // `values` change — see hook doc above.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  function isDirty(): boolean {
    return JSON.stringify(values) !== JSON.stringify(snapshotRef.current)
  }

  return { isDirty }
}
