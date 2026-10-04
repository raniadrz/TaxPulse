import { AlertOctagon, CheckCircle2, FileClock, Loader, type LucideIcon } from 'lucide-react'
import type { ObligationStatus } from '@/types/api'
import type { BadgeTone } from '@/components/ui/Badge'

/**
 * Single source of truth for how a workflow status looks. Colours follow the reserved status
 * palette (good / warning / serious / critical) and always ship with an icon + label, so meaning
 * never relies on colour alone.
 */
export const statusVisual: Record<ObligationStatus, { tone: BadgeTone; icon: LucideIcon; color: string }> = {
  SUBMITTED: { tone: 'green', icon: CheckCircle2, color: '#0ca30c' },
  IN_PROGRESS: { tone: 'amber', icon: Loader, color: '#fab219' },
  PENDING_DOCS: { tone: 'gray', icon: FileClock, color: '#ec835a' },
  OVERDUE: { tone: 'red', icon: AlertOctagon, color: '#d03b3b' },
}

/** Display order: most urgent first. */
export const statusOrder: ObligationStatus[] = ['OVERDUE', 'PENDING_DOCS', 'IN_PROGRESS', 'SUBMITTED']
