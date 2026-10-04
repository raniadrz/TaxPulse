import { Card, CardHeader } from '@/components/ui/Card'
import { obligationStatusLabel } from '@/lib/labels'
import type { ObligationStatus } from '@/types/api'
import { statusOrder, statusVisual } from '@/components/obligations/status'

/**
 * Part-to-whole of obligations by status: one stacked bar (2px surface gaps between segments,
 * rounded ends) plus a legend that doubles as the data table (icon + label + count + share).
 */
export function StatusBreakdown({ counts }: { counts: Record<ObligationStatus, number> }) {
  const total = statusOrder.reduce((sum, s) => sum + (counts[s] ?? 0), 0)

  return (
    <Card>
      <CardHeader title="Υποχρεώσεις ανά κατάσταση" description={`${total} συνολικά`} />
      <div className="px-5 py-4">
        {total === 0 ? (
          <p className="text-sm text-slate-500">Δεν υπάρχουν υποχρεώσεις ακόμη.</p>
        ) : (
          <div className="flex h-3 w-full gap-0.5" role="img" aria-label="Κατανομή υποχρεώσεων ανά κατάσταση">
            {statusOrder
              .filter((s) => counts[s] > 0)
              .map((s) => (
                <div
                  key={s}
                  className="h-full rounded transition-opacity first:rounded-l-full last:rounded-r-full hover:opacity-80"
                  style={{ width: `${(counts[s] / total) * 100}%`, backgroundColor: statusVisual[s].color }}
                  title={`${obligationStatusLabel[s]}: ${counts[s]} (${Math.round((counts[s] / total) * 100)}%)`}
                />
              ))}
          </div>
        )}

        <dl className="mt-4 space-y-2.5">
          {statusOrder.map((s) => {
            const { icon: Icon, color } = statusVisual[s]
            const share = total ? Math.round(((counts[s] ?? 0) / total) * 100) : 0
            return (
              <div key={s} className="flex items-center gap-2">
                <Icon className="size-4 shrink-0" style={{ color }} aria-hidden />
                <dt className="truncate text-sm text-slate-600">{obligationStatusLabel[s]}</dt>
                <dd className="ml-auto text-sm font-medium tabular-nums text-slate-900">
                  {counts[s] ?? 0} <span className="text-xs font-normal text-slate-400">({share}%)</span>
                </dd>
              </div>
            )
          })}
        </dl>
      </div>
    </Card>
  )
}
