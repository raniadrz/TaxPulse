import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  portalAccountService,
  portalService,
  type CreatePortalAccountRequest,
  type UpdatePortalAccountRequest,
} from '@/services/portalService'
import type { ObligationStatus, UUID } from '@/types/api'
import { queryKeys } from './queryKeys'

export function usePortalProfile() {
  return useQuery({ queryKey: queryKeys.portal.profile, queryFn: portalService.profile })
}

export function usePortalObligations(status: ObligationStatus[], sort?: string) {
  return useQuery({
    queryKey: queryKeys.portal.obligations(status, sort),
    queryFn: () => portalService.obligations(status, sort),
    placeholderData: keepPreviousData,
  })
}

/** Polls every 3s while any document is still being indexed, then stops. */
export function usePortalDocuments(page = 0) {
  return useQuery({
    queryKey: queryKeys.portal.documents(page),
    queryFn: () => portalService.documents(page),
    placeholderData: keepPreviousData,
    refetchInterval: (query) =>
      query.state.data?.content.some((d) => d.ingestionStatus === 'PENDING' || d.ingestionStatus === 'PROCESSING')
        ? 3_000
        : false,
  })
}

export function usePortalUpload() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ file, obligationId, onProgress }: { file: File; obligationId?: UUID; onProgress?: (p: number) => void }) =>
      portalService.upload(file, obligationId, onProgress),
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.portal.all })
      void queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all })
    },
  })
}

export function usePortalAccounts(clientId: UUID, enabled = true) {
  return useQuery({
    queryKey: queryKeys.portalAccounts(clientId),
    queryFn: () => portalAccountService.list(clientId),
    enabled,
  })
}

/** Every portal login of the office (Users page, ADMIN). */
export function useAllPortalAccounts(enabled: boolean) {
  return useQuery({ queryKey: queryKeys.allPortalAccounts, queryFn: portalAccountService.listAll, enabled })
}

export function useSavePortalAccount(clientId: UUID) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { userId?: UUID; create?: CreatePortalAccountRequest; update?: UpdatePortalAccountRequest }) =>
      input.userId
        ? portalAccountService.update(clientId, input.userId, input.update!)
        : portalAccountService.create(clientId, input.create!),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['portal-accounts'] }),
  })
}
