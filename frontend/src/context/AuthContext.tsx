import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { authService } from '@/services/authService'
import { UNAUTHORIZED_EVENT } from '@/services/apiClient'
import { tokenStorage } from '@/lib/tokenStorage'
import type { Role, User } from '@/types/api'
import { AuthContext, type AuthContextValue } from './auth-context'

/**
 * Holds the authenticated user. On mount it validates any stored token against /auth/me;
 * it also listens for 401 responses broadcast by the Axios client to log out automatically.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<User | null>(null)
  const [initializing, setInitializing] = useState<boolean>(() => tokenStorage.get() !== null)

  useEffect(() => {
    if (!tokenStorage.get()) return
    let cancelled = false
    authService
      .me()
      .then((me) => !cancelled && setUser(me))
      .catch(() => tokenStorage.clear())
      .finally(() => !cancelled && setInitializing(false))
    return () => {
      cancelled = true
    }
  }, [])

  const logout = useCallback(() => {
    tokenStorage.clear()
    setUser(null)
    queryClient.clear() // never leak cached data to the next user on a shared PC
  }, [queryClient])

  useEffect(() => {
    window.addEventListener(UNAUTHORIZED_EVENT, logout)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, logout)
  }, [logout])

  const login = useCallback(async (email: string, password: string) => {
    const response = await authService.login(email, password)
    tokenStorage.set(response.accessToken)
    setUser(response.user)
  }, [])

  const hasRole = useCallback((...roles: Role[]) => !!user && roles.includes(user.role), [user])

  const value = useMemo<AuthContextValue>(
    () => ({ user, initializing, login, logout, hasRole }),
    [user, initializing, login, logout, hasRole],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
