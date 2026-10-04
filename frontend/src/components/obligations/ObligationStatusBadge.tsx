import { Badge } from '@/components/ui/Badge'
import { obligationStatusLabel } from '@/lib/labels'
import type { ObligationStatus } from '@/types/api'
import { statusVisual } from './status'

export function ObligationStatusBadge({ status }: { status: ObligationStatus }) {
  const { tone, icon: Icon } = statusVisual[status]
  return (
    <Badge tone={tone}>
      <Icon className="size-3.5" aria-hidden />
      {obligationStatusLabel[status]}
    </Badge>
  )
}
