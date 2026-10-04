import { apiClient } from './apiClient'
import type { Client, ClientRequest, ClientSearchParams, ClientSummary, PageResponse, UUID } from '@/types/api'

export const clientService = {
  async search(params: ClientSearchParams): Promise<PageResponse<ClientSummary>> {
    const { data } = await apiClient.get<PageResponse<ClientSummary>>('/clients', { params })
    return data
  },

  async get(id: UUID): Promise<Client> {
    const { data } = await apiClient.get<Client>(`/clients/${id}`)
    return data
  },

  async getByAfm(afm: string): Promise<Client> {
    const { data } = await apiClient.get<Client>(`/clients/by-afm/${afm}`)
    return data
  },

  async create(request: ClientRequest): Promise<Client> {
    const { data } = await apiClient.post<Client>('/clients', request)
    return data
  },

  async update(id: UUID, request: ClientRequest): Promise<Client> {
    const { data } = await apiClient.put<Client>(`/clients/${id}`, request)
    return data
  },

  async remove(id: UUID): Promise<void> {
    await apiClient.delete(`/clients/${id}`)
  },
}
