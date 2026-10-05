import type { Role } from '@/types/api'

/** Landing page of each kind of account: clients live in the portal, staff in the office app. */
export function homePathFor(role: Role): string {
  return role === 'CLIENT' ? '/portal' : '/'
}
