import { apiClient } from './apiClient'
import type { ObligationMessage, UUID } from '@/types/api'

/** Which API the caller uses: staff routes, or the client portal's ownership-checked ones. */
export type MessageScope = 'staff' | 'portal'

const base = (scope: MessageScope, obligationId: UUID) =>
  scope === 'portal' ? `/portal/obligations/${obligationId}/messages` : `/obligations/${obligationId}/messages`

export const messageService = {
  async list(scope: MessageScope, obligationId: UUID): Promise<ObligationMessage[]> {
    const { data } = await apiClient.get<ObligationMessage[]>(base(scope, obligationId))
    return data
  },

  async post(scope: MessageScope, obligationId: UUID, body: string): Promise<ObligationMessage> {
    const { data } = await apiClient.post<ObligationMessage>(base(scope, obligationId), { body })
    return data
  },
}
