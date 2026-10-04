import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { userService, type CreateUserRequest, type UpdateUserRequest } from '@/services/userService'
import type { UUID } from '@/types/api'
import { queryKeys } from './queryKeys'

export function useUsers() {
  return useQuery({ queryKey: queryKeys.users, queryFn: userService.list })
}

export function useCreateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: CreateUserRequest) => userService.create(request),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: queryKeys.users }),
  })
}

export function useUpdateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, request }: { id: UUID; request: UpdateUserRequest }) => userService.update(id, request),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: queryKeys.users }),
  })
}
