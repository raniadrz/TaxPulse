import { useCallback, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { aiService } from '@/services/aiService'
import { getErrorMessage } from '@/lib/errors'
import type { ChatMessage, SourceRef, UUID } from '@/types/api'
import { queryKeys } from './queryKeys'

export interface UiChatMessage extends ChatMessage {
  id: string
  sources?: SourceRef[]
  error?: boolean
}

interface Options {
  clientId?: UUID
  useDocuments?: boolean
}

/**
 * Conversation state for the floating AI assistant. The full (trimmed) history is sent on every
 * turn because the backend is stateless; requests can be cancelled with `stop()`.
 */
export function useAiChat({ clientId, useDocuments }: Options = {}) {
  const [messages, setMessages] = useState<UiChatMessage[]>([])
  const [pending, setPending] = useState(false)
  const abortRef = useRef<AbortController | null>(null)

  const send = useCallback(
    async (content: string) => {
      const text = content.trim()
      if (!text || pending) return

      const userMessage: UiChatMessage = { id: crypto.randomUUID(), role: 'USER', content: text }
      const history = [...messages.filter((m) => !m.error), userMessage]
      setMessages((prev) => [...prev, userMessage])
      setPending(true)

      const controller = new AbortController()
      abortRef.current = controller
      try {
        const response = await aiService.chat(
          { messages: history.map(({ role, content: c }) => ({ role, content: c })), clientId, useDocuments },
          controller.signal,
        )
        setMessages((prev) => [
          ...prev,
          { id: crypto.randomUUID(), role: 'ASSISTANT', content: response.reply, sources: response.sources },
        ])
      } catch (error) {
        if (controller.signal.aborted) return
        setMessages((prev) => [
          ...prev,
          { id: crypto.randomUUID(), role: 'ASSISTANT', content: getErrorMessage(error), error: true },
        ])
      } finally {
        setPending(false)
        abortRef.current = null
      }
    },
    [messages, pending, clientId, useDocuments],
  )

  const stop = useCallback(() => abortRef.current?.abort(), [])
  const reset = useCallback(() => {
    abortRef.current?.abort()
    setMessages([])
  }, [])

  return { messages, pending, send, stop, reset }
}

/** Ollama reachability / model availability, refreshed every 2 minutes while the widget is open. */
export function useAiHealth(enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.aiHealth,
    queryFn: aiService.health,
    enabled,
    refetchInterval: 120_000,
  })
}
