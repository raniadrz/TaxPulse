import { Bot, FileText, User } from 'lucide-react'
import { cn } from '@/lib/cn'
import type { UiChatMessage } from '@/hooks/useAiChat'

export function ChatMessageBubble({ message }: { message: UiChatMessage }) {
  const isUser = message.role === 'USER'
  return (
    <div className={cn('flex gap-2', isUser && 'flex-row-reverse')}>
      <span
        className={cn(
          'flex size-7 shrink-0 items-center justify-center rounded-full',
          isUser ? 'bg-brand-600 text-white' : 'bg-slate-100 text-slate-600',
        )}
        aria-hidden
      >
        {isUser ? <User className="size-4" /> : <Bot className="size-4" />}
      </span>
      <div className={cn('max-w-[80%] space-y-1.5', isUser && 'items-end')}>
        <div
          className={cn(
            'whitespace-pre-wrap rounded-2xl px-3 py-2 text-sm leading-relaxed',
            isUser && 'rounded-tr-sm bg-brand-600 text-white',
            !isUser && !message.error && 'rounded-tl-sm bg-slate-100 text-slate-800',
            message.error && 'rounded-tl-sm border border-red-200 bg-red-50 text-red-800',
          )}
        >
          {message.content}
        </div>
        {message.sources && message.sources.length > 0 && (
          <ul className="space-y-0.5">
            {message.sources.map((s, i) => (
              <li key={`${s.documentId}-${s.chunkIndex}`} className="flex items-center gap-1 text-[11px] text-slate-500">
                <FileText className="size-3" aria-hidden />[{i + 1}] {s.filename} · τμήμα {s.chunkIndex + 1}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
