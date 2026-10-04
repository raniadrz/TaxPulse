import { CalendarClock, FileText, LayoutDashboard, Users, type LucideIcon } from 'lucide-react'

export interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  disabled?: boolean
}

export const navItems: NavItem[] = [
  { to: '/', label: 'Πίνακας ελέγχου', icon: LayoutDashboard },
  { to: '/clients', label: 'Πελάτες', icon: Users },
  { to: '/obligations', label: 'Φορολογικό ημερολόγιο', icon: CalendarClock },
  { to: '/documents', label: 'Έγγραφα', icon: FileText, disabled: true },
]
