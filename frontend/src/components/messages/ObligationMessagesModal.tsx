import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { MessageSquare, Send } from 'lucide-react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { Textarea } from '@/components/ui/FormField'
import { ErrorAlert } from '@/components/ui/Alert'
import { Spinner } from '@/components/ui/Spinner'
import { useMessages, usePostMessage } from '@/hooks/useMessages'
import type { MessageScope } from '@/services/messageService'
import { getErrorMessage } from '@/lib/errors'
import { formatDateTime } from '@/lib/format'
import { cn } from '@/lib/cn'
import type { UUID } from '@/types/api'

interface Props {
  obligationId: UUID
  /** Shown in the dialog title when known. */
  title?: string
  scope: MessageScope
  onClose: () => void
}

/** Accountant/client conversation about one obligation. Mounted only while open. */
export function ObligationMessagesModal({ obligationId, title, scope, onClose }: Props) {
  const messages = useMessages(scope, obligationId)
  const post = usePostMessage(scope, obligationId)
  const [draft, setDraft] = useState('')
  const endRef = useRef<HTMLDivElement>(null)
  const count = messages.data?.length ?? 0

  useEffect(() => {
    endRef.current?.scrollIntoView({ block: 'end' })
  }, [count])

  const send = () => {
    const body = draft.trim()
    if (!body || post.isPending) return
    post.mutate(body, { onSuccess: () => setDraft('') })
  }

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    send()
  }

  // Enter sends, Shift+Enter adds a new line.
  const onKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      send()
    }
  }

  const counterpart = scope === 'portal' ? 'στο γραφείο' : 'στον πελάτη'

  return (
    <Modal open onClose={onClose} title={title ? `Μηνύματα · ${title}` : 'Μηνύματα υποχρέωσης'} size="lg">
      <div className="flex min-h-[18rem] flex-col">
        <div className="max-h-[50vh] flex-1 space-y-3 overflow-y-auto pb-3">
          {messages.isLoading && <Spinner />}
          {messages.isError && <ErrorAlert>{getErrorMessage(messages.error)}</ErrorAlert>}
          {messages.data?.length === 0 && (
            <div className="flex flex-col items-center py-10 text-center text-sm text-slate-500">
              <MessageSquare className="mb-2 size-8 text-slate-300" aria-hidden />
              Δεν υπάρχουν μηνύματα. Γράψτε {counterpart} για αυτή την υποχρέωση.
            </div>
          )}
          {messages.data?.map((m) => (
            <div key={m.id} className={cn('flex', m.mine ? 'justify-end' : 'justify-start')}>
              <div className={cn('max-w-[80%] rounded-2xl px-3.5 py-2 text-sm',
                m.mine ? 'rounded-br-sm bg-brand-600 text-white' : 'rounded-bl-sm bg-slate-100 text-slate-900')}>
                {!m.mine && (
                  <div className="mb-0.5 text-xs font-semibold text-slate-600">
                    {m.authorName}{m.fromClient ? '' : ' · Γραφείο'}
                  </div>
                )}
                <p className="whitespace-pre-wrap break-words">{m.body}</p>
                <div className={cn('mt-1 text-[11px]', m.mine ? 'text-brand-100' : 'text-slate-400')}>{formatDateTime(m.createdAt)}</div>
              </div>
            </div>
          ))}
          <div ref={endRef} />
        </div>

        <form onSubmit={onSubmit} className="space-y-2 border-t border-slate-100 pt-3">
          {post.isError && <ErrorAlert>{getErrorMessage(post.error)}</ErrorAlert>}
          <div className="flex items-end gap-2">
            <Textarea rows={2} maxLength={4000} value={draft} onChange={(e) => setDraft(e.target.value)} onKeyDown={onKeyDown}
              placeholder="Γράψτε ένα μήνυμα… (Enter για αποστολή, Shift+Enter για νέα γραμμή)" aria-label="Μήνυμα" />
            <Button type="submit" icon={<Send className="size-4" />} loading={post.isPending} disabled={!draft.trim()}>
              Αποστολή
            </Button>
          </div>
        </form>
      </div>
    </Modal>
  )
}
