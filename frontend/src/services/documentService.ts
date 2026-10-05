import { apiClient } from './apiClient'
import { saveBlob } from '@/lib/download'
import type { DocumentInfo, PageResponse, UUID } from '@/types/api'

export const documentService = {
  async list(clientId: UUID, page = 0, size = 20): Promise<PageResponse<DocumentInfo>> {
    const { data } = await apiClient.get<PageResponse<DocumentInfo>>(`/clients/${clientId}/documents`, {
      params: { page, size },
    })
    return data
  },

  async upload(clientId: UUID, file: File, onProgress?: (percent: number) => void): Promise<DocumentInfo> {
    const form = new FormData()
    form.append('file', file)
    const { data } = await apiClient.post<DocumentInfo>(`/clients/${clientId}/documents`, form, {
      timeout: 120_000,
      onUploadProgress: (e) => e.total && onProgress?.(Math.round((e.loaded / e.total) * 100)),
    })
    return data
  },

  /**
   * Downloads through Axios (so the JWT header is sent) and hands the blob to the browser.
   * A plain <a href> cannot carry the Authorization header.
   */
  async download(doc: DocumentInfo): Promise<void> {
    const { data } = await apiClient.get<Blob>(`/documents/${doc.id}/download`, { responseType: 'blob', timeout: 120_000 })
    saveBlob(data, doc.originalFilename)
  },

  async reindex(id: UUID): Promise<DocumentInfo> {
    const { data } = await apiClient.post<DocumentInfo>(`/documents/${id}/reindex`)
    return data
  },

  async remove(id: UUID): Promise<void> {
    await apiClient.delete(`/documents/${id}`)
  },
}
