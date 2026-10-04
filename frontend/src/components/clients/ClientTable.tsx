import { Link } from 'react-router-dom'
import { Building2, Pencil, User } from 'lucide-react'
import { Badge } from '@/components/ui/Badge'
import { bookCategoryLabel } from '@/lib/labels'
import { formatDate } from '@/lib/format'
import type { ClientSummary } from '@/types/api'
import { ClientComplianceBadge } from './ClientComplianceBadge'

interface ClientTableProps {
  clients: ClientSummary[]
  onEdit?: (client: ClientSummary) => void
  compact?: boolean
}

/** Client list with type icon, ΑΦΜ/ΔΟΥ, book category and compliance status badges. */
export function ClientTable({ clients, onEdit, compact }: ClientTableProps) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
          <tr>
            <th scope="col" className="px-5 py-3">Πελάτης</th>
            <th scope="col" className="px-3 py-3">ΑΦΜ / ΔΟΥ</th>
            {!compact && <th scope="col" className="px-3 py-3">Βιβλία</th>}
            {!compact && <th scope="col" className="px-3 py-3">Λογιστής</th>}
            <th scope="col" className="px-3 py-3">Επόμενη λήξη</th>
            <th scope="col" className="px-3 py-3">Κατάσταση</th>
            {onEdit && <th scope="col" className="px-5 py-3"><span className="sr-only">Ενέργειες</span></th>}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 bg-white">
          {clients.map((c) => (
            <tr key={c.id} className="hover:bg-slate-50">
              <td className="px-5 py-3">
                <div className="flex items-center gap-3">
                  <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-slate-100 text-slate-500">
                    {c.clientType === 'LEGAL_ENTITY' ? <Building2 className="size-4" /> : <User className="size-4" />}
                  </span>
                  <div className="min-w-0">
                    <Link to={`/clients/${c.id}`} className="block truncate font-medium text-slate-900 hover:text-brand-600 hover:underline">{c.name}</Link>
                    <div className="truncate text-xs text-slate-500">{c.email ?? c.phone ?? (c.primaryKad && `ΚΑΔ ${c.primaryKad}`)}</div>
                  </div>
                  {!c.active && <Badge>Ανενεργός</Badge>}
                </div>
              </td>
              <td className="px-3 py-3">
                <div className="font-mono text-slate-900">{c.afm}</div>
                <div className="text-xs text-slate-500">{c.doy}</div>
              </td>
              {!compact && <td className="px-3 py-3 text-slate-600">{bookCategoryLabel[c.bookCategory]}</td>}
              {!compact && <td className="px-3 py-3 text-slate-600">{c.assignedAccountantName ?? '—'}</td>}
              <td className="px-3 py-3 text-slate-600">{formatDate(c.nextDueDate)}</td>
              <td className="px-3 py-3">
                <ClientComplianceBadge client={c} />
              </td>
              {onEdit && (
                <td className="px-5 py-3 text-right">
                  <button
                    type="button"
                    onClick={() => onEdit(c)}
                    className="rounded-md p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
                    aria-label={`Επεξεργασία ${c.name}`}
                  >
                    <Pencil className="size-4" />
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
