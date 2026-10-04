import { CheckCircle2, CircleSlash, Clock, Loader2, XCircle } from 'lucide-react'
import { Badge, type BadgeTone } from '@/components/ui/Badge'
import type { IngestionStatus } from '@/types/api'

const visual: Record<IngestionStatus, { label: string; tone: BadgeTone; icon: typeof Clock; spin?: boolean }> = {
  PENDING: { label: 'Σε αναμονή', tone: 'gray', icon: Clock },
  PROCESSING: { label: 'Ευρετηρίαση…', tone: 'blue', icon: Loader2, spin: true },
  INDEXED: { label: 'Διαθέσιμο στο AI', tone: 'green', icon: CheckCircle2 },
  FAILED: { label: 'Αποτυχία', tone: 'red', icon: XCircle },
  SKIPPED: { label: 'Χωρίς κείμενο', tone: 'gray', icon: CircleSlash },
}

export function DocumentStatusBadge({ status, error }: { status: IngestionStatus; error?: string }) {
  const { label, tone, icon: Icon, spin } = visual[status]
  return (
    <span title={error}>
      <Badge tone={tone}>
        <Icon className={spin ? 'size-3.5 animate-spin' : 'size-3.5'} aria-hidden />
        {label}
      </Badge>
    </span>
  )
}
