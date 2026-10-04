import { CalendarClock, FileText, LayoutDashboard, UserCog, Users, type LucideIcon } from 'lucide-react'
import type { Role } from '@/types/api'

export interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  disabled?: boolean
  /** Visible only to these roles (all roles when omitted). */
  roles?: Role[]
}

export const navItems: NavItem[] = [
  { to: '/', label: 'Πίνακας ελέγχου', icon: LayoutDashboard },
  { to: '/clients', label: 'Πελάτες', icon: Users },
  { to: '/obligations', label: 'Φορολογικό ημερολόγιο', icon: CalendarClock },
  { to: '/documents', label: 'Έγγραφα', icon: FileText },
  { to: '/users', label: 'Χρήστες', icon: UserCog, roles: ['ADMIN'] },
]
