import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { useMatch } from 'react-router-dom'
import { MessageSquareText, RotateCcw, ScanText, SendHorizontal, Sparkles, Square, X } from 'lucide-react'
import { useAiChat } from '@/hooks/useAiChat'
import { useClient } from '@/hooks/useClients'
import { cn } from '@/lib/cn'
import { AiHealthIndicator } from './AiHealthIndicator'
import { ChatMessageBubble } from './ChatMessageBubble'
import { ExtractionPanel } from './ExtractionPanel'
import { OPEN_AI_ASSISTANT_EVENT } from './events'

const suggestions = [
  'Ποιες είναι οι προθεσμίες ΦΠΑ για βιβλία Γ΄ κατηγορίας;',
  'Τι δικαιολογητικά χρειάζομαι για τη δήλωση Ε1;',
  'Εξήγησε τη διαφορά απλογραφικών και διπλογραφικών βιβλίων.',
]

type Tab = 'chat' | 'extract'

/**
 * Floating AI copilot connected to the Spring Boot AI endpoints (local Ollama).
 * Two tools: free chat (optionally grounded on uploaded documents via RAG) and data extraction.
 * On a client's page the chat is automatically scoped to that client (profile + open obligations,
 * and RAG restricted to the client's documents).
 */
export function AiAssistant() {
  const [open, setOpen] = useState(false)
  const [tab, setTab] = useState<Tab>('chat')
  const [useDocuments, setUseDocuments] = useState(false)
  const [input, setInput] = useState('')
  const clientMatch = useMatch('/clients/:id')
  const clientId = clientMatch?.params.id
  const { data: scopedClient } = useClient(clientId)
  const { messages, pending, send, stop, reset } = useAiChat({ useDocuments, clientId })
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const openPanel = () => {
      setOpen(true)
      setTab('chat')
    }
    window.addEventListener(OPEN_AI_ASSISTANT_EVENT, openPanel)
    return () => window.removeEventListener(OPEN_AI_ASSISTANT_EVENT, openPanel)
  }, [])

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, pending])

  const submit = (e?: FormEvent) => {
    e?.preventDefault()
    if (!input.trim()) return
    void send(input)
    setInput('')
  }

  const onKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      submit()
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="fixed bottom-6 right-6 z-40 flex size-14 items-center justify-center rounded-full bg-brand-600 text-white shadow-lg transition hover:bg-brand-700 hover:shadow-xl focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
        aria-label={open ? 'Κλείσιμο AI βοηθού' : 'Άνοιγμα AI βοηθού'}
        aria-expanded={open}
      >
        {open ? <X className="size-6" /> : <Sparkles className="size-6" />}
      </button>

      {open && (
        <section
          className="fixed bottom-24 right-4 z-40 flex h-[min(640px,calc(100vh-8rem))] w-[min(420px,calc(100vw-2rem))] flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl sm:right-6"
          aria-label="TaxPulse Copilot"
        >
          <header className="bg-gradient-to-r from-brand-700 to-brand-500 px-4 py-3 text-white">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Sparkles className="size-5" />
                <h2 className="font-semibold">TaxPulse Copilot</h2>
              </div>
              {tab === 'chat' && messages.length > 0 && (
                <button type="button" onClick={reset} className="rounded p-1 hover:bg-white/10" title="Νέα συνομιλία" aria-label="Νέα συνομιλία">
                  <RotateCcw className="size-4" />
                </button>
              )}
            </div>
            <AiHealthIndicator enabled={open} />
            <div className="mt-3 flex gap-1 rounded-lg bg-white/10 p-1 text-sm" role="tablist">
              {([
                ['chat', 'Συνομιλία', MessageSquareText],
                ['extract', 'Εξαγωγή δεδομένων', ScanText],
              ] as const).map(([key, label, Icon]) => (
                <button
                  key={key}
                  type="button"
                  role="tab"
                  aria-selected={tab === key}
                  onClick={() => setTab(key)}
                  className={cn('flex flex-1 items-center justify-center gap-1.5 rounded-md py-1', tab === key ? 'bg-white text-brand-700' : 'hover:bg-white/10')}
                >
                  <Icon className="size-4" /> {label}
                </button>
              ))}
            </div>
          </header>

          {tab === 'extract' ? (
            <ExtractionPanel />
          ) : (
            <>
              {scopedClient && (
                <div className="border-b border-slate-100 bg-brand-50 px-4 py-2 text-xs text-brand-700">
                  Πλαίσιο: <span className="font-semibold">{scopedClient.name}</span> (ΑΦΜ {scopedClient.afm}) — ο βοηθός βλέπει τα στοιχεία και τις ανοιχτές υποχρεώσεις του.
                </div>
              )}
              <div ref={scrollRef} className="flex-1 space-y-4 overflow-y-auto p-4" aria-live="polite">
                {messages.length === 0 && (
                  <div className="space-y-2">
                    <p className="text-sm text-slate-500">Ρωτήστε για φορολογικά θέματα ή για τα έγγραφα των πελατών σας. Όλη η επεξεργασία γίνεται τοπικά.</p>
                    {suggestions.map((s) => (
                      <button key={s} type="button" onClick={() => void send(s)}
                        className="block w-full rounded-lg border border-slate-200 px-3 py-2 text-left text-sm text-slate-700 hover:border-brand-300 hover:bg-brand-50">
                        {s}
                      </button>
                    ))}
                  </div>
                )}
                {messages.map((m) => <ChatMessageBubble key={m.id} message={m} />)}
                {pending && (
                  <div className="flex items-center gap-2 text-sm text-slate-500">
                    <span className="flex gap-1" aria-hidden>
                      <span className="size-1.5 animate-bounce rounded-full bg-slate-400 [animation-delay:-0.3s]" />
                      <span className="size-1.5 animate-bounce rounded-full bg-slate-400 [animation-delay:-0.15s]" />
                      <span className="size-1.5 animate-bounce rounded-full bg-slate-400" />
                    </span>
                    Ο βοηθός σκέφτεται…
                  </div>
                )}
              </div>

              <form onSubmit={submit} className="border-t border-slate-200 p-3">
                <label className="mb-2 flex items-center gap-2 text-xs text-slate-600">
                  <input type="checkbox" checked={useDocuments} onChange={(e) => setUseDocuments(e.target.checked)} />
                  Αναζήτηση στα έγγραφα του γραφείου (RAG)
                </label>
                <div className="flex items-end gap-2">
                  <textarea
                    rows={1}
                    value={input}
                    maxLength={8000}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={onKeyDown}
                    placeholder="Γράψτε την ερώτησή σας…"
                    className="max-h-32 min-h-10 flex-1 resize-none rounded-lg border-0 px-3 py-2 text-sm ring-1 ring-inset ring-slate-300 focus:ring-2 focus:ring-brand-600"
                    aria-label="Μήνυμα"
                  />
                  {pending ? (
                    <button type="button" onClick={stop} className="flex size-10 items-center justify-center rounded-lg bg-slate-200 text-slate-700 hover:bg-slate-300" aria-label="Διακοπή">
                      <Square className="size-4" />
                    </button>
                  ) : (
                    <button type="submit" disabled={!input.trim()} className="flex size-10 items-center justify-center rounded-lg bg-brand-600 text-white hover:bg-brand-700 disabled:opacity-50" aria-label="Αποστολή">
                      <SendHorizontal className="size-4" />
                    </button>
                  )}
                </div>
              </form>
            </>
          )}
        </section>
      )}
    </>
  )
}
