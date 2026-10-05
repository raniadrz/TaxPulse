import { useState, type FormEvent } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { Field, Input, Select } from '@/components/ui/FormField'
import { ErrorAlert } from '@/components/ui/Alert'
import { useCreateUser, useUpdateUser } from '@/hooks/useUsers'
import { getErrorMessage, getFieldErrors } from '@/lib/errors'
import { roleLabel } from '@/lib/labels'
import { STAFF_ROLES, type Role, type User } from '@/types/api'

const MIN_PASSWORD = 10

/**
 * Create a staff account, or edit name / role / active flag of an existing one.
 * Mounted only while open (keyed by user id) so each opening starts with fresh state.
 */
export function UserFormModal({ user, isSelf, onClose }: { user?: User; isSelf?: boolean; onClose: () => void }) {
  const isEdit = !!user
  const create = useCreateUser()
  const update = useUpdateUser()
  const mutation = isEdit ? update : create

  const [email, setEmail] = useState(user?.email ?? '')
  const [fullName, setFullName] = useState(user?.fullName ?? '')
  const [role, setRole] = useState<Role>(user?.role ?? 'ACCOUNTANT')
  const [active, setActive] = useState(user?.active ?? true)
  const [password, setPassword] = useState('')

  const errors = getFieldErrors(mutation.error)
  const passwordTooShort = !isEdit && password.length > 0 && password.length < MIN_PASSWORD

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (isEdit) {
      update.mutate({ id: user.id, request: { fullName, role, active } }, { onSuccess: onClose })
    } else {
      if (password.length < MIN_PASSWORD) return
      create.mutate({ email, fullName, role, password }, { onSuccess: onClose })
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={isEdit ? 'Επεξεργασία χρήστη' : 'Νέος χρήστης'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Άκυρο</Button>
          <Button type="submit" form="user-form" loading={mutation.isPending}>{isEdit ? 'Αποθήκευση' : 'Δημιουργία'}</Button>
        </>
      }
    >
      <form id="user-form" onSubmit={onSubmit} className="space-y-4">
        {mutation.isError && <ErrorAlert>{getErrorMessage(mutation.error)}</ErrorAlert>}
        <Field label="Email" required error={errors.email} hint={isEdit ? 'Το email σύνδεσης δεν αλλάζει.' : undefined}>
          <Input type="email" required disabled={isEdit} value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="off" />
        </Field>
        <Field label="Ονοματεπώνυμο" required error={errors.fullName}>
          <Input required maxLength={150} value={fullName} onChange={(e) => setFullName(e.target.value)} />
        </Field>
        <Field label="Ρόλος" required hint={isSelf ? 'Δεν μπορείτε να αλλάξετε τον δικό σας ρόλο.' : undefined}>
          <Select value={role} disabled={isSelf} onChange={(e) => setRole(e.target.value as Role)}>
            {STAFF_ROLES.map((r) => <option key={r} value={r}>{roleLabel[r]}</option>)}
          </Select>
        </Field>
        {!isEdit && (
          <Field label="Αρχικός κωδικός" required error={passwordTooShort ? `Τουλάχιστον ${MIN_PASSWORD} χαρακτήρες` : errors.password}>
            <Input type="password" required minLength={MIN_PASSWORD} maxLength={128} value={password}
              invalid={passwordTooShort} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" />
          </Field>
        )}
        {isEdit && !isSelf && (
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
            Ενεργός λογαριασμός (οι ανενεργοί χρήστες δεν μπορούν να συνδεθούν)
          </label>
        )}
      </form>
    </Modal>
  )
}
