// Greek locale formatting helpers.
const dateFmt = new Intl.DateTimeFormat('el-GR', { day: '2-digit', month: '2-digit', year: 'numeric' })
const dateTimeFmt = new Intl.DateTimeFormat('el-GR', { dateStyle: 'short', timeStyle: 'short' })
const currencyFmt = new Intl.NumberFormat('el-GR', { style: 'currency', currency: 'EUR' })

/** Today's calendar date in the user's time zone as yyyy-MM-dd (toISOString() would give the UTC date). */
export function todayIsoDate(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** "2026-10-20" -> "20/10/2026" (parsed as a calendar date, not shifted by time zone). */
export function formatDate(iso?: string | null): string {
  if (!iso) return '—'
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number)
  if (!y || !m || !d) return iso
  return dateFmt.format(new Date(y, m - 1, d))
}

export function formatDateTime(iso?: string | null): string {
  return iso ? dateTimeFmt.format(new Date(iso)) : '—'
}

export function formatCurrency(amount?: number | null): string {
  return amount == null ? '—' : currencyFmt.format(amount)
}

/** "σε 3 ημέρες" / "σήμερα" / "πριν 2 ημέρες". */
export function formatRelativeDays(days: number): string {
  if (days === 0) return 'σήμερα'
  if (days === 1) return 'αύριο'
  if (days === -1) return 'χθες'
  return days > 0 ? `σε ${days} ημέρες` : `πριν ${Math.abs(days)} ημέρες`
}

/** Human readable file size, e.g. 1536 -> "1.5 KB". */
export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
