import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { notificationService } from '@/services/notificationService'
import type { UUID } from '@/types/api'
import { queryKeys } from './queryKeys'

/** Polls the unread badge every minute (cheap COUNT on a partial index). */
export function useUnreadNotificationCount() {
  return useQuery({
    queryKey: queryKeys.notifications.unreadCount,
    queryFn: notificationService.unreadCount,
    refetchInterval: 60_000,
  })
}

export function useNotifications(enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.notifications.list,
    queryFn: () => notificationService.list(false, 10),
    enabled,
  })
}

export function useMarkNotificationsRead() {
  const queryClient = useQueryClient()
  const invalidate = () => void queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all })
  return {
    markOne: useMutation({ mutationFn: (id: UUID) => notificationService.markRead(id), onSuccess: invalidate }),
    markAll: useMutation({ mutationFn: notificationService.markAllRead, onSuccess: invalidate }),
  }
}
