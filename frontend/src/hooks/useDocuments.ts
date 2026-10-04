import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { documentService } from '@/services/documentService'
import type { UUID } from '@/types/api'
import { queryKeys } from './queryKeys'

/** Polls every 3s while any document is still being indexed, then stops. */
export function useDocuments(clientId: UUID | undefined, page = 0) {
  return useQuery({
    queryKey: queryKeys.documents.list(clientId ?? '', page),
    queryFn: () => documentService.list(clientId!, page),
    enabled: !!clientId,
    placeholderData: keepPreviousData,
    refetchInterval: (query) =>
      query.state.data?.content.some((d) => d.ingestionStatus === 'PENDING' || d.ingestionStatus === 'PROCESSING')
        ? 3_000
        : false,
  })
}

function useInvalidateDocuments() {
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: queryKeys.documents.all })
    void queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all })
  }
}

export function useUploadDocument() {
  const invalidate = useInvalidateDocuments()
  return useMutation({
    mutationFn: ({ clientId, file, onProgress }: { clientId: UUID; file: File; onProgress?: (p: number) => void }) =>
      documentService.upload(clientId, file, onProgress),
    onSettled: invalidate,
  })
}

export function useReindexDocument() {
  const invalidate = useInvalidateDocuments()
  return useMutation({ mutationFn: (id: UUID) => documentService.reindex(id), onSuccess: invalidate })
}

export function useDeleteDocument() {
  const invalidate = useInvalidateDocuments()
  return useMutation({ mutationFn: (id: UUID) => documentService.remove(id), onSuccess: invalidate })
}
