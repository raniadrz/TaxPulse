import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { Spinner } from '@/components/ui/Spinner'
import type { Role } from '@/types/api'

/** Guards nested routes: requires authentication and, optionally, one of the given roles. */
export function ProtectedRoute({ roles }: { roles?: Role[] }) {
  const { user, initializing, hasRole } = useAuth()
  const location = useLocation()

  if (initializing) return <Spinner className="h-full" label="Έλεγχος σύνδεσης…" />
  if (!user) return <Navigate to="/login" replace state={{ from: location }} />
  if (roles && !hasRole(...roles)) return <Navigate to="/" replace />
  return <Outlet />
}
