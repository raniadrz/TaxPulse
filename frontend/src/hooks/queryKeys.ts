import type { ClientSearchParams, ObligationSearchParams, UUID } from '@/types/api'

/** Centralised React Query keys: one place to reason about cache invalidation. */
export const queryKeys = {
  dashboard: ['dashboard'] as const,
  clients: {
    all: ['clients'] as const,
    list: (params: ClientSearchParams) => ['clients', 'list', params] as const,
    detail: (id: UUID) => ['clients', 'detail', id] as const,
  },
  obligations: {
    all: ['obligations'] as const,
    list: (params: ObligationSearchParams) => ['obligations', 'list', params] as const,
  },
  documents: {
    all: ['documents'] as const,
    list: (clientId: UUID, page: number) => ['documents', clientId, page] as const,
  },
  notifications: {
    all: ['notifications'] as const,
    unreadCount: ['notifications', 'unread-count'] as const,
    list: ['notifications', 'list'] as const,
  },
  users: ['users'] as const,
  aiHealth: ['ai', 'health'] as const,
}
