import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { credentialService, type CredentialScope } from '@/services/credentialService'
import type { CredentialRequest, UUID } from '@/types/api'
import { queryKeys } from './queryKeys'

const scopeKey = (scope: CredentialScope) => (scope.kind === 'portal' ? 'portal' : scope.clientId)

export function useCredentials(scope: CredentialScope) {
  return useQuery({ queryKey: queryKeys.credentials(scopeKey(scope)), queryFn: () => credentialService.list(scope) })
}

export function useCredentialLog(scope: CredentialScope, enabled: boolean) {
  return useQuery({ queryKey: queryKeys.credentialLog(scopeKey(scope)), queryFn: () => credentialService.log(scope), enabled })
}

export function useCredentialMutations(scope: CredentialScope) {
  const queryClient = useQueryClient()
  // Also refreshes the access log, since every change (and reveal) is recorded there.
  const invalidate = () => void queryClient.invalidateQueries({ queryKey: queryKeys.credentials(scopeKey(scope)) })
  return {
    save: useMutation({
      mutationFn: ({ id, request }: { id?: UUID; request: CredentialRequest }) =>
        id ? credentialService.update(scope, id, request) : credentialService.create(scope, request),
      onSuccess: invalidate,
    }),
    remove: useMutation({ mutationFn: (id: UUID) => credentialService.remove(scope, id), onSuccess: invalidate }),
    reveal: useMutation({ mutationFn: (id: UUID) => credentialService.reveal(scope, id), onSuccess: invalidate }),
  }
}
