import type { AppNotification, Role } from '@/types/api'

/**
 * Where a notification leads when clicked. Conversation notifications open the thread itself
 * (`?messages=<obligationId>`), the rest the page where the related item lives.
 */
export function notificationLink(n: AppNotification, role: Role): string | null {
  if (role === 'CLIENT') {
    switch (n.type) {
      case 'MESSAGE':
        return n.obligationId ? `/portal/obligations?messages=${n.obligationId}` : '/portal/obligations'
      case 'DOCUMENT_RECEIVED':
      case 'DOCUMENT_PROCESSED':
        return '/portal/documents'
      case 'CREDENTIALS_UPDATED':
        return '/portal/credentials'
      case 'STATUS_CHANGED':
      case 'DEADLINE_UPCOMING':
      case 'DEADLINE_OVERDUE':
        return '/portal/obligations'
      default:
        return null
    }
  }
  if (!n.clientId) return null
  if (n.type === 'DOCUMENT_PROCESSED') return `/documents?clientId=${n.clientId}`
  if (n.type === 'MESSAGE' && n.obligationId) return `/clients/${n.clientId}?messages=${n.obligationId}`
  return `/clients/${n.clientId}`
}
