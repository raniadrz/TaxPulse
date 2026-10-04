import { useQuery } from '@tanstack/react-query'
import { dashboardService } from '@/services/dashboardService'
import { queryKeys } from './queryKeys'

export function useDashboardStats() {
  return useQuery({ queryKey: queryKeys.dashboard, queryFn: dashboardService.stats })
}
