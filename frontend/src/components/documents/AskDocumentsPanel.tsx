import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { FileText, Sparkles } from 'lucide-react'
import { Card, CardHeader } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Textarea } from '@/components/ui/FormField'
import { ErrorAlert } from '@/components/ui/Alert'
import { getErrorMessage } from '@/lib/errors'
import type { ChatResponse } from '@/types/api'

/** RAG question box over one client's indexed documents; `onAsk` carries the scope. */
export function AskDocumentsPanel({ onAsk, indexedCount }: { onAsk: (question: string) => Promise<ChatResponse>; indexedCount: number }) {
  const [question, setQuestion] = useState('')
  const ask = useMutation({ mutationFn: onAsk })

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (question.trim()) ask.mutate(question.trim())
  }

  return (
    <Card>
      <CardHeader title="Ερώτηση στα έγγραφα" description={`${indexedCount} ευρετηριασμένα έγγραφα · τοπική επεξεργασία`} />
      <form onSubmit={onSubmit} className="space-y-3 p-5">
        <Textarea
          rows={3}
          maxLength={2000}
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="π.χ. Ποιο είναι το συνολικό ποσό ΦΠΑ στα τιμολόγια Σεπτεμβρίου;"
          aria-label="Ερώτηση"
        />
        <Button type="submit" icon={<Sparkles className="size-4" />} loading={ask.isPending} disabled={!question.trim() || indexedCount === 0} className="w-full">
          Ερώτηση
        </Button>
        {indexedCount === 0 && <p className="text-xs text-slate-500">Ανεβάστε PDF ή TXT για να ενεργοποιηθεί η αναζήτηση.</p>}
        {ask.isError && <ErrorAlert>{getErrorMessage(ask.error)}</ErrorAlert>}
        {ask.data && (
          <div className="space-y-2 rounded-lg bg-slate-50 p-3">
            <p className="whitespace-pre-wrap text-sm leading-relaxed text-slate-800">{ask.data.reply}</p>
            {ask.data.sources.length > 0 && (
              <ul className="space-y-0.5 border-t border-slate-200 pt-2">
                {ask.data.sources.map((s, i) => (
                  <li key={`${s.documentId}-${s.chunkIndex}`} className="flex items-center gap-1 text-xs text-slate-500">
                    <FileText className="size-3" aria-hidden />[{i + 1}] {s.filename} · τμήμα {s.chunkIndex + 1}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </form>
    </Card>
  )
}
