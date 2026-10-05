import { useRef, useState } from 'react'
import { MessageSquare, Upload } from 'lucide-react'
import { DueDateCell } from '@/components/obligations/DueDateCell'
import { ObligationStatusBadge } from '@/components/obligations/ObligationStatusBadge'
import { Button } from '@/components/ui/Button'
import { usePortalUpload } from '@/hooks/usePortal'
import { formatCurrency, formatDate } from '@/lib/format'
import { getErrorMessage } from '@/lib/errors'
import type { PortalObligation, UUID } from '@/types/api'

/** The client's obligations, with a one-click upload of supporting documents and the conversation with the office. */
export function PortalObligationTable({ obligations, onMessages }: {
  obligations: PortalObligation[]
  onMessages: (obligation: PortalObligation) => void
}) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [target, setTarget] = useState<UUID | null>(null)
  const [feedback, setFeedback] = useState<{ id: UUID; text: string; error?: boolean } | null>(null)
  const upload = usePortalUpload()

  const pick = (id: UUID) => {
    setTarget(id)
    setFeedback(null)
    inputRef.current?.click()
  }

  const onFiles = async (files: FileList | null) => {
    if (!files?.length || !target) return
    const id = target
    try {
      for (const file of Array.from(files)) await upload.mutateAsync({ file, obligationId: id })
      setFeedback({ id, text: files.length === 1 ? 'Το αρχείο στάλθηκε στο γραφείο.' : `${files.length} αρχεία στάλθηκαν στο γραφείο.` })
    } catch (err) {
      setFeedback({ id, text: getErrorMessage(err), error: true })
    } finally {
      setTarget(null)
    }
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
          <tr>
            <th scope="col" className="px-5 py-3">Υποχρέωση</th>
            <th scope="col" className="px-3 py-3">Προθεσμία</th>
            <th scope="col" className="px-3 py-3">Κατάσταση</th>
            <th scope="col" className="px-3 py-3 text-right">Ποσό</th>
            <th scope="col" className="px-5 py-3"><span className="sr-only">Ενέργειες</span></th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 bg-white">
          {obligations.map((o) => {
            const open = o.status !== 'SUBMITTED'
            return (
              <tr key={o.id} className="align-top hover:bg-slate-50">
                <td className="px-5 py-3">
                  <div className="font-medium text-slate-900">{o.title}</div>
                  <div className="text-xs text-slate-500">
                    {o.obligationTypeLabel}
                    {o.periodStart && o.periodEnd && ` · περίοδος ${formatDate(o.periodStart)} – ${formatDate(o.periodEnd)}`}
                  </div>
                  {o.submissionRef && <div className="text-xs text-slate-500">Αρ. πρωτοκόλλου: {o.submissionRef}</div>}
                  {feedback?.id === o.id && (
                    <div className={feedback.error ? 'mt-1 text-xs text-red-600' : 'mt-1 text-xs text-emerald-700'}>{feedback.text}</div>
                  )}
                </td>
                <td className="whitespace-nowrap px-3 py-3"><DueDateCell dueDate={o.dueDate} daysUntilDue={o.daysUntilDue} status={o.status} /></td>
                <td className="px-3 py-3"><ObligationStatusBadge status={o.status} /></td>
                <td className="whitespace-nowrap px-3 py-3 text-right tabular-nums text-slate-900">{o.amount != null ? formatCurrency(o.amount) : '—'}</td>
                <td className="px-5 py-3 text-right">
                  <div className="flex items-center justify-end gap-2">
                    <Button size="sm" variant="ghost" className="whitespace-nowrap" icon={<MessageSquare className="size-4" />} onClick={() => onMessages(o)}
                      aria-label={`Μηνύματα (${o.messageCount})`}>
                      Μηνύματα{o.messageCount > 0 && ` (${o.messageCount})`}
                    </Button>
                    {open && (
                      <Button size="sm" variant={o.status === 'PENDING_DOCS' ? 'primary' : 'secondary'} icon={<Upload className="size-4" />}
                        loading={upload.isPending && target === o.id} onClick={() => pick(o.id)}>
                        Δικαιολογητικά
                      </Button>
                    )}
                  </div>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
      <input ref={inputRef} type="file" multiple accept=".pdf,.txt,.csv,.md,.png,.jpg,.jpeg,.xlsx,.docx" className="hidden"
        onChange={(e) => {
          void onFiles(e.target.files)
          e.target.value = ''
        }} />
    </div>
  )
}
