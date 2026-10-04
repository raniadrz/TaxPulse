import { apiClient } from './apiClient'
import type { DashboardStats } from '@/types/api'

export const dashboardService = {
  async stats(): Promise<DashboardStats> {
    const { data } = await apiClient.get<DashboardStats>('/dashboard/stats')
    return data
  },
}
