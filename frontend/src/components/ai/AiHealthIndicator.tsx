import { useAiHealth } from '@/hooks/useAiChat'
import { cn } from '@/lib/cn'

/** Small status pill: is the local Ollama server reachable and is the chat model pulled? */
export function AiHealthIndicator({ enabled }: { enabled: boolean }) {
  const { data, isLoading } = useAiHealth(enabled)
  const ok = !!data?.reachable && data.chatModelAvailable
  const label = isLoading
    ? 'Έλεγχος…'
    : !data?.reachable
      ? 'Ollama εκτός σύνδεσης'
      : !data.chatModelAvailable
        ? `Λείπει το μοντέλο ${data.chatModel}`
        : data.chatModel

  return (
    <span className="flex items-center gap-1.5 text-xs text-brand-100" title={data?.error ?? data?.baseUrl}>
      <span className={cn('size-2 rounded-full', isLoading ? 'bg-slate-300' : ok ? 'bg-emerald-400' : 'bg-red-400')} aria-hidden />
      {label}
    </span>
  )
}
