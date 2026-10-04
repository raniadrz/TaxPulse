import type { LucideIcon } from 'lucide-react'
import { Card } from '@/components/ui/Card'
import { cn } from '@/lib/cn'

interface StatCardProps {
  label: string
  value: number | string
  icon: LucideIcon
  hint?: string
  /** Highlights the tile (e.g. overdue > 0); text stays in ink colours, only the icon chip is tinted. */
  emphasis?: 'default' | 'critical' | 'warning'
}

/** KPI tile: a single headline number needs no chart. */
export function StatCard({ label, value, icon: Icon, hint, emphasis = 'default' }: StatCardProps) {
  return (
    <Card className="p-4 sm:p-5">
      <div className="flex items-start justify-between">
        <p className="text-sm font-medium text-slate-500">{label}</p>
        <span
          className={cn(
            'flex size-9 items-center justify-center rounded-lg',
            emphasis === 'critical' && 'bg-red-50 text-red-600',
            emphasis === 'warning' && 'bg-amber-50 text-amber-600',
            emphasis === 'default' && 'bg-brand-50 text-brand-600',
          )}
        >
          <Icon className="size-5" aria-hidden />
        </span>
      </div>
      <p className="mt-2 text-2xl sm:text-3xl font-semibold tabular-nums tracking-tight text-slate-900">{value}</p>
      {hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
    </Card>
  )
}
