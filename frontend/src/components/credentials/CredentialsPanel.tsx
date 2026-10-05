import { useEffect, useState, type FormEvent } from 'react'
import { Check, Copy, Eye, EyeOff, History, KeyRound, Pencil, Plus, ShieldCheck, Trash2 } from 'lucide-react'
import { Card, CardHeader } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { Field, Input, Select } from '@/components/ui/FormField'
import { ErrorAlert } from '@/components/ui/Alert'
import { Spinner } from '@/components/ui/Spinner'
import { useCredentialLog, useCredentialMutations, useCredentials } from '@/hooks/useCredentials'
import type { CredentialScope } from '@/services/credentialService'
import { copyText } from '@/lib/clipboard'
import { getErrorMessage, getFieldErrors } from '@/lib/errors'
import { formatDateTime } from '@/lib/format'
import { credentialKindLabel } from '@/lib/labels'
import type { ClientCredential, CredentialKind, UUID } from '@/types/api'

/** How long a revealed password stays on screen. */
const REVEAL_MS = 30_000

const actionLabel = { VIEW: 'προβολή', CREATE: 'προσθήκη', UPDATE: 'αλλαγή', DELETE: 'διαγραφή' } as const

/**
 * Stored logins for public services (TAXISnet, e-ΕΦΚΑ), shared by the client and the office.
 * Passwords stay hidden until revealed; every reveal and change is recorded in the access log.
 */
export function CredentialsPanel({ scope }: { scope: CredentialScope }) {
  const isPortal = scope.kind === 'portal'
  const credentials = useCredentials(scope)
  const { save, remove, reveal } = useCredentialMutations(scope)
  const [editing, setEditing] = useState<{ open: boolean; credential?: ClientCredential }>({ open: false })
  const [revealed, setRevealed] = useState<Record<UUID, string>>({})
  const [copied, setCopied] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [showLog, setShowLog] = useState(false)
  const log = useCredentialLog(scope, showLog)

  // Hide revealed passwords again after a while (and when leaving the page).
  useEffect(() => {
    if (Object.keys(revealed).length === 0) return
    const t = setTimeout(() => setRevealed({}), REVEAL_MS)
    return () => clearTimeout(t)
  }, [revealed])

  const toggleReveal = (c: ClientCredential) => {
    setActionError(null)
    if (revealed[c.id] !== undefined) {
      setRevealed((r) => Object.fromEntries(Object.entries(r).filter(([id]) => id !== c.id)))
      return
    }
    reveal.mutate(c.id, {
      onSuccess: (password) => setRevealed((r) => ({ ...r, [c.id]: password })),
      onError: (err) => setActionError(getErrorMessage(err)),
    })
  }

  const copy = async (key: string, text: string) => {
    try {
      await copyText(text)
      setCopied(key)
      setTimeout(() => setCopied(null), 1500)
    } catch (err) {
      setActionError(getErrorMessage(err))
    }
  }

  const onDelete = (c: ClientCredential) => {
    if (window.confirm(`Διαγραφή των κωδικών ${c.kindLabel}${c.label ? ` (${c.label})` : ''};`)) {
      remove.mutate(c.id, { onError: (err) => setActionError(getErrorMessage(err)) })
    }
  }

  return (
    <Card>
      <CardHeader
        title={isPortal ? 'Οι κωδικοί μου' : 'Κωδικοί TAXISnet / e-ΕΦΚΑ'}
        description={isPortal
          ? 'Το γραφείο τους χρησιμοποιεί για τις υποβολές σας. Κάθε προβολή καταγράφεται.'
          : 'Κρυπτογραφημένοι. Κάθε προβολή καταγράφεται και τη βλέπει ο πελάτης.'}
        action={<Button size="sm" variant="secondary" icon={<Plus className="size-4" />} onClick={() => setEditing({ open: true })}>Προσθήκη</Button>}
      />
      {actionError && <div className="px-5 pb-3"><ErrorAlert>{actionError}</ErrorAlert></div>}
      {credentials.isLoading && <Spinner />}
      {credentials.isError && <div className="px-5 pb-4"><ErrorAlert>{getErrorMessage(credentials.error)}</ErrorAlert></div>}
      {credentials.data?.length === 0 && (
        <p className="flex items-center gap-2 px-5 pb-4 text-sm text-slate-500">
          <KeyRound className="size-4" aria-hidden />
          {isPortal ? 'Δεν έχετε καταχωρίσει κωδικούς. Προσθέστε τους κωδικούς TAXISnet για να μπορεί το γραφείο να κάνει τις υποβολές σας.'
            : 'Δεν έχουν καταχωριστεί κωδικοί.'}
        </p>
      )}
      {credentials.data && credentials.data.length > 0 && (
        <ul className="divide-y divide-slate-100 text-sm">
          {credentials.data.map((c) => {
            const shown = revealed[c.id]
            return (
              <li key={c.id} className="px-5 py-3">
                <div className="flex items-start gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="font-medium text-slate-900">{c.kindLabel}{c.label && <span className="font-normal text-slate-500"> · {c.label}</span>}</div>
                    <dl className="mt-1.5 grid grid-cols-[6.5rem_1fr] gap-x-2 gap-y-1">
                      <dt className="text-slate-500">Όνομα χρήστη</dt>
                      <dd className="flex min-w-0 items-center gap-1">
                        <span className="break-all font-mono text-slate-900">{c.username}</span>
                        <IconButton label="Αντιγραφή ονόματος χρήστη" onClick={() => void copy(`${c.id}-u`, c.username)}
                          icon={copied === `${c.id}-u` ? Check : Copy} />
                      </dd>
                      <dt className="text-slate-500">Κωδικός</dt>
                      <dd className="flex min-w-0 items-center gap-1">
                        <span className="break-all font-mono text-slate-900">{shown ?? '••••••••••'}</span>
                        <IconButton label={shown !== undefined ? 'Απόκρυψη κωδικού' : 'Εμφάνιση κωδικού'} onClick={() => toggleReveal(c)}
                          icon={shown !== undefined ? EyeOff : Eye} busy={reveal.isPending && reveal.variables === c.id} />
                        {shown !== undefined && (
                          <IconButton label="Αντιγραφή κωδικού" onClick={() => void copy(`${c.id}-p`, shown)} icon={copied === `${c.id}-p` ? Check : Copy} />
                        )}
                      </dd>
                    </dl>
                    <p className="mt-1.5 text-xs text-slate-400">
                      Τελευταία αλλαγή {formatDateTime(c.updatedAt)}{c.updatedByName && ` από ${c.updatedByName}`}
                      {c.updatedByClient ? (isPortal ? ' (εσείς)' : ' (πελάτης)') : ' (γραφείο)'}
                    </p>
                  </div>
                  <div className="flex shrink-0 gap-1">
                    <IconButton label="Επεξεργασία" onClick={() => setEditing({ open: true, credential: c })} icon={Pencil} />
                    <IconButton label="Διαγραφή" onClick={() => onDelete(c)} icon={Trash2} danger />
                  </div>
                </div>
              </li>
            )
          })}
        </ul>
      )}

      <div className="border-t border-slate-100 px-5 py-3">
        <button type="button" onClick={() => setShowLog((v) => !v)} className="inline-flex items-center gap-1.5 text-sm font-medium text-brand-600 hover:underline">
          <History className="size-4" aria-hidden /> {showLog ? 'Απόκρυψη ιστορικού' : 'Ιστορικό πρόσβασης'}
        </button>
        {showLog && (
          <div className="mt-2">
            {log.isLoading && <Spinner />}
            {log.data?.length === 0 && <p className="text-sm text-slate-500">Καμία καταγραφή.</p>}
            <ul className="space-y-1 text-xs text-slate-600">
              {log.data?.map((l, i) => (
                <li key={`${l.createdAt}-${i}`} className="flex gap-2">
                  <span className="shrink-0 text-slate-400">{formatDateTime(l.createdAt)}</span>
                  <span>
                    <span className="font-medium text-slate-800">{l.userName}</span>
                    {l.byClient ? ' (πελάτης)' : ' (γραφείο)'} · {actionLabel[l.action]} · {l.kindLabel}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      {editing.open && (
        <CredentialModal key={editing.credential?.id ?? 'new'} credential={editing.credential}
          saving={save.isPending} error={save.error}
          onSubmit={(request) => save.mutate({ id: editing.credential?.id, request }, { onSuccess: () => setEditing({ open: false }) })}
          onClose={() => {
            save.reset()
            setEditing({ open: false })
          }} />
      )}
    </Card>
  )
}

function CredentialModal({ credential, saving, error, onSubmit, onClose }: {
  credential?: ClientCredential
  saving: boolean
  error: unknown
  onSubmit: (request: { kind: CredentialKind; label?: string; username: string; password?: string }) => void
  onClose: () => void
}) {
  const isEdit = !!credential
  const [kind, setKind] = useState<CredentialKind>(credential?.kind ?? 'TAXISNET')
  const [label, setLabel] = useState(credential?.label ?? '')
  const [username, setUsername] = useState(credential?.username ?? '')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const errors = getFieldErrors(error)

  const submit = (e: FormEvent) => {
    e.preventDefault()
    onSubmit({ kind, label: label || undefined, username, password: password || undefined })
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={isEdit ? 'Αλλαγή κωδικών' : 'Νέοι κωδικοί'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Άκυρο</Button>
          <Button type="submit" form="credential-form" loading={saving}>{isEdit ? 'Αποθήκευση' : 'Προσθήκη'}</Button>
        </>
      }
    >
      <form id="credential-form" onSubmit={submit} className="space-y-4" autoComplete="off">
        {!!error && <ErrorAlert>{getErrorMessage(error)}</ErrorAlert>}
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Υπηρεσία" required>
            <Select value={kind} onChange={(e) => setKind(e.target.value as CredentialKind)}>
              {Object.entries(credentialKindLabel).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </Select>
          </Field>
          <Field label="Περιγραφή" hint={kind === 'OTHER' ? 'π.χ. «myBusinessSupport»' : 'Προαιρετικό'}>
            <Input maxLength={100} value={label} onChange={(e) => setLabel(e.target.value)} />
          </Field>
        </div>
        <Field label="Όνομα χρήστη" required error={errors.username}>
          <Input required maxLength={255} value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="off" spellCheck={false} />
        </Field>
        <Field label="Κωδικός" required={!isEdit} error={errors.password}
          hint={isEdit ? 'Αφήστε κενό για να μείνει ο ίδιος.' : undefined}>
          <div className="relative">
            <Input type={showPassword ? 'text' : 'password'} required={!isEdit} maxLength={255} value={password}
              onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" spellCheck={false} className="pr-10" />
            <button type="button" onClick={() => setShowPassword((v) => !v)} aria-label={showPassword ? 'Απόκρυψη' : 'Εμφάνιση'}
              className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 text-slate-400 hover:text-slate-700">
              {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
            </button>
          </div>
        </Field>
        <p className="flex items-start gap-2 text-xs text-slate-500">
          <ShieldCheck className="mt-0.5 size-4 shrink-0 text-emerald-600" aria-hidden />
          Ο κωδικός αποθηκεύεται κρυπτογραφημένος. Τον βλέπουν μόνο ο πελάτης και οι λογιστές του γραφείου, και κάθε προβολή καταγράφεται.
        </p>
      </form>
    </Modal>
  )
}

function IconButton({ label, onClick, icon: Icon, danger, busy }: {
  label: string
  onClick: () => void
  icon: typeof Copy
  danger?: boolean
  busy?: boolean
}) {
  return (
    <button type="button" onClick={onClick} title={label} aria-label={label} disabled={busy}
      className={`rounded-md p-1 text-slate-400 hover:bg-slate-100 disabled:opacity-50 ${danger ? 'hover:text-red-600' : 'hover:text-slate-700'}`}>
      <Icon className="size-4" />
    </button>
  )
}
