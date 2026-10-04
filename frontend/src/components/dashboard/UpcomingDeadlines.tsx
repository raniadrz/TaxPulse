import { Link } from 'react-router-dom'
import { CalendarCheck } from 'lucide-react'
import { Card, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ObligationStatusBadge } from '@/components/obligations/ObligationStatusBadge'
import { DueDateCell } from '@/components/obligations/DueDateCell'
import type { Obligation } from '@/types/api'

export function UpcomingDeadlines({ items }: { items: Obligation[] }) {
  return (
    <Card>
      <CardHeader
        title="Επόμενες προθεσμίες"
        description="Ανοιχτές και εκπρόθεσμες υποχρεώσεις"
        action={
          <Link to="/obligations" className="text-sm font-medium text-brand-600 hover:underline">
            Όλες
          </Link>
        }
      />
      {items.length === 0 ? (
        <EmptyState icon={<CalendarCheck className="size-10" />} title="Καμία εκκρεμότητα" description="Όλες οι υποχρεώσεις έχουν υποβληθεί." />
      ) : (
        <ul className="divide-y divide-slate-100">
          {items.map((o) => (
            <li key={o.id} className="flex items-center gap-4 px-5 py-3">
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-slate-900">{o.client.name}</p>
                <p className="truncate text-xs text-slate-500">
                  {o.obligationTypeLabel} · {o.title}
                </p>
              </div>
              <DueDateCell dueDate={o.dueDate} daysUntilDue={o.daysUntilDue} status={o.status} />
              <div className="hidden w-48 justify-end sm:flex">
                <ObligationStatusBadge status={o.status} />
              </div>
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}
