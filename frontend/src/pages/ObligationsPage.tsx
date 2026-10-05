import { useState } from 'react'
import { CalendarClock, Plus } from 'lucide-react'
import { PageHeader } from '@/components/layout/PageHeader'
import { ObligationTable } from '@/components/obligations/ObligationTable'
import { ObligationMessagesModal } from '@/components/messages/ObligationMessagesModal'
import { ObligationFormModal } from '@/components/obligations/ObligationFormModal'
import { ReminderEmailModal } from '@/components/obligations/ReminderEmailModal'
import { statusOrder } from '@/components/obligations/status'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Input, Select } from '@/components/ui/FormField'
import { Pagination } from '@/components/ui/Pagination'
import { Spinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/Alert'
import { useObligations } from '@/hooks/useObligations'
import { useObligationActions } from '@/hooks/useObligationActions'
import { useAuth } from '@/hooks/useAuth'
import { getErrorMessage } from '@/lib/errors'
import { obligationStatusLabel, obligationTypeLabel } from '@/lib/labels'
import { cn } from '@/lib/cn'
import type { ObligationStatus, ObligationType } from '@/types/api'

export default function ObligationsPage() {
  const { hasRole } = useAuth()
  const [statuses, setStatuses] = useState<ObligationStatus[]>(['OVERDUE', 'PENDING_DOCS', 'IN_PROGRESS'])
  const [type, setType] = useState<ObligationType | ''>('')
  const [dueFrom, setDueFrom] = useState('')
  const [dueTo, setDueTo] = useState('')
  const [mine, setMine] = useState(false)
  const [page, setPage] = useState(0)
  const [creating, setCreating] = useState(false)

  const obligations = useObligations({
    status: statuses,
    type: type || undefined,
    dueFrom: dueFrom || undefined,
    dueTo: dueTo || undefined,
    mine: mine || undefined,
    page,
    size: 20,
    sort: 'dueDate,asc',
  })
  const { onStatusChange, statusError, busyId, emailFor, setEmailFor, messagesFor, setMessagesFor } = useObligationActions()

  const toggleStatus = (s: ObligationStatus) => {
    setPage(0)
    setStatuses((prev) => (prev.includes(s) ? prev.filter((x) => x !== s) : [...prev, s]))
  }

  return (
    <>
      <PageHeader
        title="Φορολογικό ημερολόγιο"
        description="Προθεσμίες ΦΠΑ, εισοδήματος, ΑΠΔ, myDATA, ΓΕΜΗ και λοιπών υποχρεώσεων"
        actions={hasRole('ADMIN', 'ACCOUNTANT') && <Button icon={<Plus className="size-4" />} onClick={() => setCreating(true)}>Νέα υποχρέωση</Button>}
      />

      <Card>
        {/* Filters: one row above the table */}
        <div className="flex flex-wrap items-center gap-2 border-b border-slate-100 p-4">
          {statusOrder.map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => toggleStatus(s)}
              aria-pressed={statuses.includes(s)}
              className={cn(
                'rounded-full px-3 py-1 text-xs font-medium ring-1 ring-inset transition-colors',
                statuses.includes(s) ? 'bg-brand-600 text-white ring-brand-600' : 'bg-white text-slate-600 ring-slate-300 hover:bg-slate-50',
              )}
            >
              {obligationStatusLabel[s]}
            </button>
          ))}
          <Select className="w-44 py-1.5" value={type} onChange={(e) => { setType(e.target.value as ObligationType | ''); setPage(0) }} aria-label="Τύπος">
            <option value="">Όλοι οι τύποι</option>
            {Object.entries(obligationTypeLabel).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </Select>
          <Input type="date" className="w-40 py-1.5" value={dueFrom} onChange={(e) => { setDueFrom(e.target.value); setPage(0) }} aria-label="Λήξη από" />
          <Input type="date" className="w-40 py-1.5" value={dueTo} onChange={(e) => { setDueTo(e.target.value); setPage(0) }} aria-label="Λήξη έως" />
          <label className="ml-auto flex items-center gap-2 text-sm text-slate-600">
            <input type="checkbox" checked={mine} onChange={(e) => { setMine(e.target.checked); setPage(0) }} />
            Μόνο οι δικές μου
          </label>
        </div>

        {statusError && <div className="p-4"><ErrorAlert>{getErrorMessage(statusError)}</ErrorAlert></div>}
        {obligations.isLoading && <Spinner />}
        {obligations.isError && <div className="p-4"><ErrorAlert>{getErrorMessage(obligations.error)}</ErrorAlert></div>}
        {obligations.data && obligations.data.content.length === 0 && (
          <EmptyState icon={<CalendarClock className="size-10" />} title="Καμία υποχρέωση" description="Δεν υπάρχουν υποχρεώσεις για τα επιλεγμένα φίλτρα." />
        )}
        {obligations.data && obligations.data.content.length > 0 && (
          <>
            <ObligationTable
              obligations={obligations.data.content}
              onStatusChange={onStatusChange}
              onDraftEmail={setEmailFor}
              onMessages={setMessagesFor}
              busyId={busyId}
            />
            <Pagination page={obligations.data} onPageChange={setPage} />
          </>
        )}
      </Card>

      {creating && <ObligationFormModal onClose={() => setCreating(false)} />}
      <ReminderEmailModal obligation={emailFor} onClose={() => setEmailFor(null)} />
      {messagesFor && (
        <ObligationMessagesModal scope="staff" obligationId={messagesFor.id} title={`${messagesFor.client.name} · ${messagesFor.title}`}
          onClose={() => setMessagesFor(null)} />
      )}
    </>
  )
}
