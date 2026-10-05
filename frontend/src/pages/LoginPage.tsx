import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Activity, Lock, Mail } from 'lucide-react'
import { useAuth } from '@/hooks/useAuth'
import { getErrorMessage } from '@/lib/errors'
import { Button } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/FormField'
import { ErrorAlert } from '@/components/ui/Alert'

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const redirectTo = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/'
  if (user) return <Navigate to={redirectTo} replace />

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(email, password)
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-full items-center justify-center bg-gradient-to-br from-slate-900 via-slate-800 to-brand-700 px-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow-2xl">
        <div className="mb-6 flex flex-col items-center text-center">
          <span className="mb-3 flex size-12 items-center justify-center rounded-xl bg-brand-600 text-white">
            <Activity className="size-7" />
          </span>
          <h1 className="text-xl font-semibold">TaxPulse AI</h1>
          <p className="text-sm text-slate-500">Λογιστικό γραφείο & portal πελατών</p>
        </div>

        <form onSubmit={onSubmit} className="space-y-4">
          {error && <ErrorAlert>{error}</ErrorAlert>}
          <Field label="Email">
            <div className="relative">
              <Mail className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
              <Input type="email" autoComplete="username" required value={email} onChange={(e) => setEmail(e.target.value)} className="pl-9" />
            </div>
          </Field>
          <Field label="Κωδικός">
            <div className="relative">
              <Lock className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
              <Input type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} className="pl-9" />
            </div>
          </Field>
          <Button type="submit" className="w-full" loading={submitting}>
            Σύνδεση
          </Button>
        </form>
        <p className="mt-5 border-t border-slate-100 pt-4 text-center text-xs text-slate-500">
          Πελάτης του γραφείου; Συνδεθείτε με το email και τον κωδικό που σας έστειλε ο λογιστής σας.
        </p>
      </div>
    </div>
  )
}
