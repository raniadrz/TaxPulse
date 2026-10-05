import { apiClient } from './apiClient'
import type { ClientCredential, CredentialLogEntry, CredentialRequest, UUID } from '@/types/api'

/** Office routes take the client id; the portal works on the logged-in client. */
export type CredentialScope = { kind: 'staff'; clientId: UUID } | { kind: 'portal' }

const base = (scope: CredentialScope) =>
  scope.kind === 'portal' ? '/portal/credentials' : `/clients/${scope.clientId}/credentials`

export const credentialService = {
  async list(scope: CredentialScope): Promise<ClientCredential[]> {
    const { data } = await apiClient.get<ClientCredential[]>(base(scope))
    return data
  },

  /** Audited: every call is recorded in the client's access log. */
  async reveal(scope: CredentialScope, id: UUID): Promise<string> {
    const { data } = await apiClient.post<{ password: string }>(`${base(scope)}/${id}/reveal`)
    return data.password
  },

  async create(scope: CredentialScope, request: CredentialRequest): Promise<ClientCredential> {
    const { data } = await apiClient.post<ClientCredential>(base(scope), request)
    return data
  },

  async update(scope: CredentialScope, id: UUID, request: CredentialRequest): Promise<ClientCredential> {
    const { data } = await apiClient.put<ClientCredential>(`${base(scope)}/${id}`, request)
    return data
  },

  async remove(scope: CredentialScope, id: UUID): Promise<void> {
    await apiClient.delete(`${base(scope)}/${id}`)
  },

  async log(scope: CredentialScope): Promise<CredentialLogEntry[]> {
    const { data } = await apiClient.get<CredentialLogEntry[]>(`${base(scope)}/log`)
    return data
  },
}
