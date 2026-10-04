import { formatDate, formatRelativeDays } from '@/lib/format'
import { cn } from '@/lib/cn'
import type { ObligationStatus } from '@/types/api'

/** Due date with a relative hint, emphasised when the deadline is close or missed. */
export function DueDateCell({ dueDate, daysUntilDue, status }: { dueDate: string; daysUntilDue: number; status: ObligationStatus }) {
  const done = status === 'SUBMITTED'
  return (
    <div className="leading-tight">
      <div className="text-sm text-slate-900">{formatDate(dueDate)}</div>
      {!done && (
        <div
          className={cn(
            'text-xs',
            daysUntilDue < 0 ? 'font-medium text-red-700' : daysUntilDue <= 3 ? 'font-medium text-amber-700' : 'text-slate-500',
          )}
        >
          {formatRelativeDays(daysUntilDue)}
        </div>
      )}
    </div>
  )
}
