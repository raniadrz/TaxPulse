import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { clientService } from '@/services/clientService'
import type { ClientRequest, ClientSearchParams, UUID } from '@/types/api'
import { queryKeys } from './queryKeys'

export function useClients(params: ClientSearchParams) {
  return useQuery({
    queryKey: queryKeys.clients.list(params),
    queryFn: () => clientService.search(params),
    placeholderData: keepPreviousData, // no flicker while paging / typing
  })
}

export function useClient(id: UUID | undefined) {
  return useQuery({
    queryKey: queryKeys.clients.detail(id ?? ''),
    queryFn: () => clientService.get(id!),
    enabled: !!id,
  })
}

export function useSaveClient() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, request }: { id?: UUID; request: ClientRequest }) =>
      id ? clientService.update(id, request) : clientService.create(request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.clients.all })
      void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard })
    },
  })
}

export function useDeleteClient() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: UUID) => clientService.remove(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.clients.all })
      void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard })
    },
  })
}
