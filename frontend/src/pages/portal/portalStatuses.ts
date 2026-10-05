import type { ObligationStatus } from '@/types/api'

/** Statuses that still need something done, most urgent first in the UI. */
export const OPEN_STATUSES: ObligationStatus[] = ['OVERDUE', 'PENDING_DOCS', 'IN_PROGRESS']
export const DONE_STATUSES: ObligationStatus[] = ['SUBMITTED']
