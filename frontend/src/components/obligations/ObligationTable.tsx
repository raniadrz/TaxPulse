import { Link } from 'react-router-dom'
import { Mail } from 'lucide-react'
import { Select } from '@/components/ui/FormField'
import { formatCurrency } from '@/lib/format'
import { obligationStatusLabel } from '@/lib/labels'
import type { Obligation, ObligationStatus } from '@/types/api'
import { DueDateCell } from './DueDateCell'
import { ObligationStatusBadge } from './ObligationStatusBadge'

/** Allowed manual transitions (mirrors ObligationStatus.canTransitionTo on the backend). */
const transitions: Record<ObligationStatus, ObligationStatus[]> = {
  PENDING_DOCS: ['IN_PROGRESS', 'SUBMITTED'],
  IN_PROGRESS: ['PENDING_DOCS', 'SUBMITTED'],
  OVERDUE: ['SUBMITTED'],
  SUBMITTED: ['IN_PROGRESS'],
}

interface ObligationTableProps {
  obligations: Obligation[]
  onStatusChange: (obligation: Obligation, status: ObligationStatus) => void
  onDraftEmail: (obligation: Obligation) => void
  busyId?: string
  /** Omit the client column (e.g. on a client's own page). */
  hideClient?: boolean
}

export function ObligationTable({ obligations, onStatusChange, onDraftEmail, busyId, hideClient }: ObligationTableProps) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
          <tr>
            <th scope="col" className="px-5 py-3">Υποχρέωση</th>
            {!hideClient && <th scope="col" className="px-3 py-3">Πελάτης</th>}
            <th scope="col" className="px-3 py-3">Προθεσμία</th>
            <th scope="col" className="px-3 py-3 text-right">Ποσό</th>
            <th scope="col" className="px-3 py-3">Κατάσταση</th>
            <th scope="col" className="px-3 py-3">Ενέργεια</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 bg-white">
          {obligations.map((o) => (
            <tr key={o.id} className="hover:bg-slate-50">
              <td className="px-5 py-3">
                <div className="font-medium text-slate-900">{o.title}</div>
                <div className="text-xs text-slate-500">
                  {o.obligationTypeLabel}
                  {o.assignedTo && ` · ${o.assignedTo.fullName}`}
                </div>
              </td>
              {!hideClient && (
                <td className="px-3 py-3">
                  <Link to={`/clients/${o.client.id}`} className="text-slate-900 hover:text-brand-600 hover:underline">{o.client.name}</Link>
                  <div className="font-mono text-xs text-slate-500">{o.client.afm}</div>
                </td>
              )}
              <td className="px-3 py-3">
                <DueDateCell dueDate={o.dueDate} daysUntilDue={o.daysUntilDue} status={o.status} />
              </td>
              <td className="px-3 py-3 text-right tabular-nums text-slate-700">{formatCurrency(o.amount)}</td>
              <td className="px-3 py-3">
                <ObligationStatusBadge status={o.status} />
              </td>
              <td className="px-3 py-3">
                <div className="flex items-center gap-1">
                  <Select
                    aria-label="Αλλαγή κατάστασης"
                    className="w-40 py-1 text-xs"
                    value=""
                    disabled={busyId === o.id}
                    onChange={(e) => e.target.value && onStatusChange(o, e.target.value as ObligationStatus)}
                  >
                    <option value="">Μετάβαση σε…</option>
                    {transitions[o.status].map((s) => (
                      <option key={s} value={s}>{obligationStatusLabel[s]}</option>
                    ))}
                  </Select>
                  {o.status !== 'SUBMITTED' && (
                    <button
                      type="button"
                      onClick={() => onDraftEmail(o)}
                      className="rounded-md p-1.5 text-slate-400 hover:bg-brand-50 hover:text-brand-600"
                      title="Σύνταξη email υπενθύμισης με AI"
                      aria-label="Σύνταξη email υπενθύμισης με AI"
                    >
                      <Mail className="size-4" />
                    </button>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
