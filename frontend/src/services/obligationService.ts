import { apiClient } from './apiClient'
import type {
  Obligation,
  ObligationRequest,
  ObligationSearchParams,
  ObligationStatus,
  PageResponse,
  UUID,
} from '@/types/api'

export const obligationService = {
  async search(params: ObligationSearchParams): Promise<PageResponse<Obligation>> {
    const { data } = await apiClient.get<PageResponse<Obligation>>('/obligations', { params })
    return data
  },

  async create(request: ObligationRequest): Promise<Obligation> {
    const { data } = await apiClient.post<Obligation>('/obligations', request)
    return data
  },

  async update(id: UUID, request: ObligationRequest): Promise<Obligation> {
    const { data } = await apiClient.put<Obligation>(`/obligations/${id}`, request)
    return data
  },

  async changeStatus(id: UUID, status: ObligationStatus, submissionRef?: string): Promise<Obligation> {
    const { data } = await apiClient.patch<Obligation>(`/obligations/${id}/status`, { status, submissionRef })
    return data
  },

  async remove(id: UUID): Promise<void> {
    await apiClient.delete(`/obligations/${id}`)
  },
}
