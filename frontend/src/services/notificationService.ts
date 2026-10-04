import { apiClient } from './apiClient'
import type { AppNotification, PageResponse, UUID } from '@/types/api'

export const notificationService = {
  async list(unreadOnly = false, size = 10): Promise<PageResponse<AppNotification>> {
    const { data } = await apiClient.get<PageResponse<AppNotification>>('/notifications', {
      params: { unreadOnly, size },
    })
    return data
  },

  async unreadCount(): Promise<number> {
    const { data } = await apiClient.get<{ count: number }>('/notifications/unread-count')
    return data.count
  },

  async markRead(id: UUID): Promise<void> {
    await apiClient.patch(`/notifications/${id}/read`)
  },

  async markAllRead(): Promise<void> {
    await apiClient.post('/notifications/read-all')
  },
}
