import { AlertOctagon, CheckCircle2, Clock } from 'lucide-react'
import { Badge } from '@/components/ui/Badge'
import type { ClientSummary } from '@/types/api'

/** Roll-up status of a client from its obligation counters. */
export function ClientComplianceBadge({ client }: { client: ClientSummary }) {
  if (client.overdueObligations > 0) {
    return (
      <Badge tone="red">
        <AlertOctagon className="size-3.5" aria-hidden />
        {client.overdueObligations} εκπρόθεσμες
      </Badge>
    )
  }
  if (client.openObligations > 0) {
    return (
      <Badge tone="amber">
        <Clock className="size-3.5" aria-hidden />
        {client.openObligations} σε εκκρεμότητα
      </Badge>
    )
  }
  return (
    <Badge tone="green">
      <CheckCircle2 className="size-3.5" aria-hidden />
      Ενήμερος
    </Badge>
  )
}
