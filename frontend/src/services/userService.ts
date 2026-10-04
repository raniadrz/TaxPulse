import { apiClient } from './apiClient'
import type { Role, User, UUID } from '@/types/api'

export interface CreateUserRequest {
  email: string
  fullName: string
  role: Role
  password: string
}

export interface UpdateUserRequest {
  fullName: string
  role: Role
  active: boolean
}

export const userService = {
  async list(): Promise<User[]> {
    const { data } = await apiClient.get<User[]>('/users')
    return data
  },

  async create(request: CreateUserRequest): Promise<User> {
    const { data } = await apiClient.post<User>('/users', request)
    return data
  },

  async update(id: UUID, request: UpdateUserRequest): Promise<User> {
    const { data } = await apiClient.put<User>(`/users/${id}`, request)
    return data
  },
}
