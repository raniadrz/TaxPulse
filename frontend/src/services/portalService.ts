import { AI_TIMEOUT_MS, apiClient } from './apiClient'
import { saveBlob } from '@/lib/download'
import type {
  ChatResponse,
  DocumentInfo,
  ObligationStatus,
  PageResponse,
  PortalAccountSummary,
  PortalObligation,
  PortalProfile,
  User,
  UUID,
} from '@/types/api'

/** Client portal endpoints: always scoped server-side to the logged-in client. */
export const portalService = {
  async profile(): Promise<PortalProfile> {
    const { data } = await apiClient.get<PortalProfile>('/portal/profile')
    return data
  },

  async obligations(status: ObligationStatus[], sort = 'dueDate,asc', size = 50): Promise<PageResponse<PortalObligation>> {
    const { data } = await apiClient.get<PageResponse<PortalObligation>>('/portal/obligations', {
      params: { status, sort, size },
    })
    return data
  },

  async documents(page = 0, size = 20): Promise<PageResponse<DocumentInfo>> {
    const { data } = await apiClient.get<PageResponse<DocumentInfo>>('/portal/documents', { params: { page, size } })
    return data
  },

  async upload(file: File, obligationId?: UUID, onProgress?: (percent: number) => void): Promise<DocumentInfo> {
    const form = new FormData()
    form.append('file', file)
    const { data } = await apiClient.post<DocumentInfo>('/portal/documents', form, {
      params: { obligationId },
      timeout: 120_000,
      onUploadProgress: (e) => e.total && onProgress?.(Math.round((e.loaded / e.total) * 100)),
    })
    return data
  },

  /** Same blob hand-off as documentService.download, through the ownership-checked portal route. */
  async download(doc: DocumentInfo): Promise<void> {
    const { data } = await apiClient.get<Blob>(`/portal/documents/${doc.id}/download`, { responseType: 'blob', timeout: 120_000 })
    saveBlob(data, doc.originalFilename)
  },

  async ask(question: string): Promise<ChatResponse> {
    const { data } = await apiClient.post<ChatResponse>('/portal/ai/ask', { question }, { timeout: AI_TIMEOUT_MS })
    return data
  },
}

export interface CreatePortalAccountRequest {
  email: string
  fullName: string
  password: string
}

export interface UpdatePortalAccountRequest {
  fullName: string
  active: boolean
  password?: string
}

/** Staff-side management of a client's portal logins. */
export const portalAccountService = {
  async listAll(): Promise<PortalAccountSummary[]> {
    const { data } = await apiClient.get<PortalAccountSummary[]>('/portal-accounts')
    return data
  },

  async list(clientId: UUID): Promise<User[]> {
    const { data } = await apiClient.get<User[]>(`/clients/${clientId}/portal-accounts`)
    return data
  },

  async create(clientId: UUID, request: CreatePortalAccountRequest): Promise<User> {
    const { data } = await apiClient.post<User>(`/clients/${clientId}/portal-accounts`, request)
    return data
  },

  async update(clientId: UUID, userId: UUID, request: UpdatePortalAccountRequest): Promise<User> {
    const { data } = await apiClient.put<User>(`/clients/${clientId}/portal-accounts/${userId}`, request)
    return data
  },
}
