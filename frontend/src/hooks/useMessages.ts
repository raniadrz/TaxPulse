import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { messageService, type MessageScope } from '@/services/messageService'
import type { UUID } from '@/types/api'
import { queryKeys } from './queryKeys'

/** Conversation of one obligation; refreshed every 15s while it is on screen. */
export function useMessages(scope: MessageScope, obligationId: UUID) {
  return useQuery({
    queryKey: queryKeys.messages(obligationId),
    queryFn: () => messageService.list(scope, obligationId),
    refetchInterval: 15_000,
  })
}

export function usePostMessage(scope: MessageScope, obligationId: UUID) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: string) => messageService.post(scope, obligationId, body),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.messages(obligationId) })
      // Message counters on the obligation lists.
      void queryClient.invalidateQueries({ queryKey: scope === 'portal' ? queryKeys.portal.all : queryKeys.obligations.all })
    },
  })
}
