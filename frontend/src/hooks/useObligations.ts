import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { obligationService } from '@/services/obligationService'
import type { ObligationRequest, ObligationSearchParams, ObligationStatus, UUID } from '@/types/api'
import { queryKeys } from './queryKeys'

export function useObligations(params: ObligationSearchParams) {
  return useQuery({
    queryKey: queryKeys.obligations.list(params),
    queryFn: () => obligationService.search(params),
    placeholderData: keepPreviousData,
  })
}

/** Status changes affect obligations, client counters and the dashboard: invalidate all three. */
function useInvalidateWorkflow() {
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: queryKeys.obligations.all })
    void queryClient.invalidateQueries({ queryKey: queryKeys.clients.all })
    void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard })
  }
}

export function useChangeObligationStatus() {
  const invalidate = useInvalidateWorkflow()
  return useMutation({
    mutationFn: ({ id, status, submissionRef }: { id: UUID; status: ObligationStatus; submissionRef?: string }) =>
      obligationService.changeStatus(id, status, submissionRef),
    onSuccess: invalidate,
  })
}

export function useSaveObligation() {
  const invalidate = useInvalidateWorkflow()
  return useMutation({
    mutationFn: ({ id, request }: { id?: UUID; request: ObligationRequest }) =>
      id ? obligationService.update(id, request) : obligationService.create(request),
    onSuccess: invalidate,
  })
}
