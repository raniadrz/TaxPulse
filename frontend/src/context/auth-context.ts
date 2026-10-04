import { createContext } from 'react'
import type { Role, User } from '@/types/api'

export interface AuthContextValue {
  user: User | null
  /** True while the stored token is being validated on first load. */
  initializing: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  hasRole: (...roles: Role[]) => boolean
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
