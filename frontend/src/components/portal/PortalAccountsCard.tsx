import { useState, type FormEvent } from 'react'
import { KeyRound, Pencil, Plus } from 'lucide-react'
import { Card, CardHeader } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { Field, Input } from '@/components/ui/FormField'
import { ErrorAlert } from '@/components/ui/Alert'
import { Spinner } from '@/components/ui/Spinner'
import { usePortalAccounts, useSavePortalAccount } from '@/hooks/usePortal'
import { PortalInvitationModal, type PortalCredentials } from './PortalInvitationModal'
import { getErrorMessage, getFieldErrors } from '@/lib/errors'
import { formatDateTime } from '@/lib/format'
import type { User, UUID } from '@/types/api'

const MIN_PASSWORD = 10

/** Logins that let the client (or its representatives) use the client portal. */
export function PortalAccountsCard({ clientId, clientName, canEdit }: { clientId: UUID; clientName: string; canEdit: boolean }) {
  const accounts = usePortalAccounts(clientId)
  const [editing, setEditing] = useState<{ open: boolean; user?: User }>({ open: false })
  const [invitation, setInvitation] = useState<PortalCredentials | null>(null)

  return (
    <Card>
      <CardHeader
        title="Πρόσβαση στο portal"
        description="Ο πελάτης βλέπει μόνο τις δικές του προθεσμίες και έγγραφα."
        action={canEdit && (
          <Button size="sm" variant="secondary" icon={<Plus className="size-4" />} onClick={() => setEditing({ open: true })}>
            Λογαριασμός
          </Button>
        )}
      />
      {accounts.isLoading && <Spinner />}
      {accounts.isError && <div className="px-5 pb-4"><ErrorAlert>{getErrorMessage(accounts.error)}</ErrorAlert></div>}
      {accounts.data?.length === 0 && (
        <p className="flex items-center gap-2 px-5 pb-4 text-sm text-slate-500">
          <KeyRound className="size-4" aria-hidden /> Δεν έχει δοθεί πρόσβαση.
        </p>
      )}
      {accounts.data && accounts.data.length > 0 && (
        <ul className="divide-y divide-slate-100 text-sm">
          {accounts.data.map((u) => (
            <li key={u.id} className="flex items-center gap-3 px-5 py-2.5">
              <div className="min-w-0">
                <div className="font-medium text-slate-900">{u.fullName}</div>
                <div className="truncate text-xs text-slate-500">
                  {u.email} · {u.lastLoginAt ? `σύνδεση ${formatDateTime(u.lastLoginAt)}` : 'δεν έχει συνδεθεί'}
                </div>
              </div>
              <span className="ml-auto">{u.active ? <Badge tone="green">Ενεργός</Badge> : <Badge>Ανενεργός</Badge>}</span>
              {canEdit && (
                <button type="button" onClick={() => setEditing({ open: true, user: u })}
                  className="rounded-md p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-700" aria-label={`Επεξεργασία ${u.fullName}`}>
                  <Pencil className="size-4" />
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
      {editing.open && (
        <PortalAccountModal key={editing.user?.id ?? 'new'} clientId={clientId} user={editing.user}
          onClose={() => setEditing({ open: false })}
          onCredentials={(c) => {
            setEditing({ open: false })
            setInvitation(c)
          }} />
      )}
      {invitation && <PortalInvitationModal credentials={invitation} clientName={clientName} onClose={() => setInvitation(null)} />}
    </Card>
  )
}

function PortalAccountModal({ clientId, user, onClose, onCredentials }: {
  clientId: UUID
  user?: User
  onClose: () => void
  /** Called after a save that set a password, so it can be handed to the client. */
  onCredentials: (credentials: PortalCredentials) => void
}) {
  const isEdit = !!user
  const save = useSavePortalAccount(clientId)
  const [email, setEmail] = useState(user?.email ?? '')
  const [fullName, setFullName] = useState(user?.fullName ?? '')
  const [active, setActive] = useState(user?.active ?? true)
  const [password, setPassword] = useState('')

  const errors = getFieldErrors(save.error)
  const passwordTooShort = password.length > 0 && password.length < MIN_PASSWORD

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (passwordTooShort || (!isEdit && !password)) return
    if (isEdit) {
      save.mutate({ userId: user.id, update: { fullName, active, password: password || undefined } }, {
        onSuccess: (saved) => (password ? onCredentials({ fullName, email: saved.email, password, reset: true }) : onClose()),
      })
    } else {
      save.mutate({ create: { email, fullName, password } }, {
        onSuccess: (saved) => onCredentials({ fullName, email: saved.email, password, reset: false }),
      })
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={isEdit ? 'Λογαριασμός portal' : 'Νέος λογαριασμός portal'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Άκυρο</Button>
          <Button type="submit" form="portal-account-form" loading={save.isPending}>{isEdit ? 'Αποθήκευση' : 'Δημιουργία'}</Button>
        </>
      }
    >
      <form id="portal-account-form" onSubmit={onSubmit} className="space-y-4">
        {save.isError && <ErrorAlert>{getErrorMessage(save.error)}</ErrorAlert>}
        <Field label="Email σύνδεσης" required error={errors.email} hint={isEdit ? 'Το email σύνδεσης δεν αλλάζει.' : undefined}>
          <Input type="email" required disabled={isEdit} value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="off" />
        </Field>
        <Field label="Ονοματεπώνυμο" required error={errors.fullName}>
          <Input required maxLength={150} value={fullName} onChange={(e) => setFullName(e.target.value)} />
        </Field>
        <Field label={isEdit ? 'Νέος κωδικός' : 'Αρχικός κωδικός'} required={!isEdit}
          error={passwordTooShort ? `Τουλάχιστον ${MIN_PASSWORD} χαρακτήρες` : errors.password}
          hint={isEdit ? 'Αφήστε κενό για να μην αλλάξει.' : 'Μετά τη δημιουργία θα δείτε έτοιμη πρόσκληση για τον πελάτη.'}>
          <Input type="password" required={!isEdit} minLength={MIN_PASSWORD} maxLength={128} value={password}
            invalid={passwordTooShort} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" />
        </Field>
        {isEdit && (
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
            Ενεργός λογαριασμός (η απενεργοποίηση αποσυνδέει αμέσως τον πελάτη)
          </label>
        )}
      </form>
    </Modal>
  )
}
