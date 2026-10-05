import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Card, CardHeader } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/FormField'
import { ErrorAlert } from '@/components/ui/Alert'
import { authService } from '@/services/authService'
import { getErrorMessage } from '@/lib/errors'

const MIN_PASSWORD = 10

/** Replaces the caller's password, e.g. the initial one handed out by the office. */
export function ChangePasswordCard() {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const change = useMutation({
    mutationFn: () => authService.changePassword(current, next),
    onSuccess: () => {
      setCurrent('')
      setNext('')
      setConfirm('')
    },
  })

  const tooShort = next.length > 0 && next.length < MIN_PASSWORD
  const mismatch = confirm.length > 0 && confirm !== next

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (next.length < MIN_PASSWORD || next !== confirm) return
    change.mutate()
  }

  return (
    <Card>
      <CardHeader title="Αλλαγή κωδικού" />
      <form onSubmit={onSubmit} className="space-y-3 px-5 pb-5">
        {change.isError && <ErrorAlert>{getErrorMessage(change.error)}</ErrorAlert>}
        {change.isSuccess && <p className="rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-800">Ο κωδικός άλλαξε.</p>}
        <Field label="Τρέχων κωδικός" required>
          <Input type="password" required autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} />
        </Field>
        <Field label="Νέος κωδικός" required error={tooShort ? `Τουλάχιστον ${MIN_PASSWORD} χαρακτήρες` : undefined}>
          <Input type="password" required minLength={MIN_PASSWORD} maxLength={128} autoComplete="new-password" value={next}
            invalid={tooShort} onChange={(e) => setNext(e.target.value)} />
        </Field>
        <Field label="Επιβεβαίωση νέου κωδικού" required error={mismatch ? 'Οι κωδικοί δεν ταιριάζουν' : undefined}>
          <Input type="password" required autoComplete="new-password" value={confirm} invalid={mismatch} onChange={(e) => setConfirm(e.target.value)} />
        </Field>
        <Button type="submit" className="w-full" loading={change.isPending}
          disabled={!current || next.length < MIN_PASSWORD || next !== confirm}>
          Αλλαγή κωδικού
        </Button>
      </form>
    </Card>
  )
}
