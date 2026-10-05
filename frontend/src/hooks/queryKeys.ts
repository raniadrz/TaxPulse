import type { ClientSearchParams, ObligationSearchParams, ObligationStatus, UUID } from '@/types/api'

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
  portalAccounts: (clientId: UUID) => ['portal-accounts', clientId] as const,
  allPortalAccounts: ['portal-accounts', 'all'] as const,
  messages: (obligationId: UUID) => ['messages', obligationId] as const,
  credentials: (scopeKey: string) => ['credentials', scopeKey] as const,
  credentialLog: (scopeKey: string) => ['credentials', scopeKey, 'log'] as const,
  portal: {
    all: ['portal'] as const,
    profile: ['portal', 'profile'] as const,
    obligations: (status: ObligationStatus[], sort?: string) => ['portal', 'obligations', status, sort] as const,
    documents: (page: number) => ['portal', 'documents', page] as const,
  },
  aiHealth: ['ai', 'health'] as const,
}
