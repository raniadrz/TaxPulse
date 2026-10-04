import { useState } from 'react'
import { useChangeObligationStatus } from './useObligations'
import type { Obligation, ObligationStatus } from '@/types/api'

/**
 * Row actions shared by every obligations table: workflow transitions (asking for the submission
 * protocol number on SUBMITTED) and the AI reminder e-mail dialog target.
 */
export function useObligationActions() {
  const changeStatus = useChangeObligationStatus()
  const [emailFor, setEmailFor] = useState<Obligation | null>(null)

  const onStatusChange = (o: Obligation, status: ObligationStatus) => {
    const submissionRef =
      status === 'SUBMITTED' ? window.prompt('Αριθμός πρωτοκόλλου υποβολής (προαιρετικό):') ?? undefined : undefined
    changeStatus.mutate({ id: o.id, status, submissionRef })
  }

  return {
    onStatusChange,
    statusError: changeStatus.isError ? changeStatus.error : null,
    busyId: changeStatus.isPending ? changeStatus.variables?.id : undefined,
    emailFor,
    setEmailFor,
  }
}
