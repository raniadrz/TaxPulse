import { AI_TIMEOUT_MS, apiClient } from './apiClient'
import type {
  ChatRequest,
  ChatResponse,
  EmailTone,
  ExtractionResponse,
  OllamaHealth,
  ReminderEmailResponse,
  UUID,
} from '@/types/api'

/** Calls the Spring Boot AI endpoints, which proxy to the local Ollama server. */
export const aiService = {
  async chat(request: ChatRequest, signal?: AbortSignal): Promise<ChatResponse> {
    const { data } = await apiClient.post<ChatResponse>('/ai/chat', request, { timeout: AI_TIMEOUT_MS, signal })
    return data
  },

  async extract(text: string): Promise<ExtractionResponse> {
    const { data } = await apiClient.post<ExtractionResponse>('/ai/extract', { text }, { timeout: AI_TIMEOUT_MS })
    return data
  },

  async draftReminderEmail(obligationId: UUID, tone: EmailTone = 'FORMAL', additionalInstructions?: string) {
    const { data } = await apiClient.post<ReminderEmailResponse>(
      '/ai/reminder-email',
      { obligationId, tone, additionalInstructions },
      { timeout: AI_TIMEOUT_MS },
    )
    return data
  },

  async askDocuments(question: string, clientId?: UUID): Promise<ChatResponse> {
    const { data } = await apiClient.post<ChatResponse>(
      '/ai/documents/ask',
      { question, clientId },
      { timeout: AI_TIMEOUT_MS },
    )
    return data
  },

  async health(): Promise<OllamaHealth> {
    const { data } = await apiClient.get<OllamaHealth>('/ai/health')
    return data
  },
}
