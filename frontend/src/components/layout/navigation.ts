import {
  Building2,
  CalendarClock,
  FileText,
  FolderOpen,
  LayoutDashboard,
  ListTodo,
  Sparkles,
  UserCog,
  Users,
  type LucideIcon,
} from 'lucide-react'
import { STAFF_ROLES, type Role } from '@/types/api'

export interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  disabled?: boolean
  /** Match the path exactly (for section roots such as "/" or "/portal"). */
  end?: boolean
  /** Visible only to these roles (all roles when omitted). */
  roles?: Role[]
}

const CLIENT: Role[] = ['CLIENT']

export const navItems: NavItem[] = [
  // Office staff
  { to: '/', label: 'Πίνακας ελέγχου', icon: LayoutDashboard, end: true, roles: STAFF_ROLES },
  { to: '/clients', label: 'Πελάτες', icon: Users, roles: STAFF_ROLES },
  { to: '/obligations', label: 'Φορολογικό ημερολόγιο', icon: CalendarClock, roles: STAFF_ROLES },
  { to: '/documents', label: 'Έγγραφα', icon: FileText, roles: STAFF_ROLES },
  { to: '/users', label: 'Χρήστες', icon: UserCog, roles: ['ADMIN'] },
  // Client portal
  { to: '/portal', label: 'Επισκόπηση', icon: LayoutDashboard, end: true, roles: CLIENT },
  { to: '/portal/obligations', label: 'Οι υποχρεώσεις μου', icon: ListTodo, roles: CLIENT },
  { to: '/portal/documents', label: 'Τα έγγραφά μου', icon: FolderOpen, roles: CLIENT },
  { to: '/portal/assistant', label: 'Ρωτήστε το AI', icon: Sparkles, roles: CLIENT },
  { to: '/portal/profile', label: 'Τα στοιχεία μου', icon: Building2, roles: CLIENT },
]
