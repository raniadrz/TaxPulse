import { useState, type FormEvent } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { Field, Input, Select, Textarea } from '@/components/ui/FormField'
import { ErrorAlert } from '@/components/ui/Alert'
import { Spinner } from '@/components/ui/Spinner'
import { useClient, useSaveClient } from '@/hooks/useClients'
import { isValidAfm } from '@/lib/afm'
import { getErrorMessage, getFieldErrors } from '@/lib/errors'
import { bookCategoryLabel, clientTypeLabel } from '@/lib/labels'
import type { ActivityCode, BookCategory, Client, ClientRequest, ClientType, UUID } from '@/types/api'

interface ClientFormModalProps {
  clientId?: UUID
  onClose: () => void
}

const emptyForm: ClientRequest = {
  clientType: 'LEGAL_ENTITY',
  afm: '',
  doy: '',
  name: '',
  bookCategory: 'C',
  activityCodes: [],
  representatives: [],
  address: {},
}

function toRequest(existing: Client): ClientRequest {
  return {
    clientType: existing.clientType,
    afm: existing.afm,
    doy: existing.doy,
    name: existing.name,
    tradeName: existing.tradeName,
    legalForm: existing.legalForm,
    bookCategory: existing.bookCategory,
    gemiNumber: existing.gemiNumber,
    email: existing.email,
    phone: existing.phone,
    mobile: existing.mobile,
    address: existing.address ?? {},
    assignedAccountantId: existing.assignedAccountant?.id,
    notes: existing.notes,
    active: existing.active,
    activityCodes: existing.activityCodes,
    representatives: existing.representatives,
  }
}

/**
 * Create / edit a client. Mount it only while open (and key it by client id) so every opening
 * starts from fresh state. In edit mode it waits for the client to load before rendering the form.
 */
export function ClientFormModal({ clientId, onClose }: ClientFormModalProps) {
  const { data: existing, isLoading, error } = useClient(clientId)
  if (!clientId) return <ClientForm onClose={onClose} initial={emptyForm} />
  if (isLoading || error || !existing) {
    return (
      <Modal open onClose={onClose} title="Επεξεργασία πελάτη">
        {error ? <ErrorAlert>{getErrorMessage(error)}</ErrorAlert> : <Spinner />}
      </Modal>
    )
  }
  return <ClientForm onClose={onClose} clientId={clientId} initial={toRequest(existing)} />
}

function ClientForm({ clientId, initial, onClose }: { clientId?: UUID; initial: ClientRequest; onClose: () => void }) {
  const isEdit = !!clientId
  const save = useSaveClient()
  const [form, setForm] = useState<ClientRequest>(initial)
  const [afmTouched, setAfmTouched] = useState(false)

  const set = <K extends keyof ClientRequest>(key: K, value: ClientRequest[K]) => setForm((f) => ({ ...f, [key]: value }))
  const codes = form.activityCodes ?? []
  const setCodes = (next: ActivityCode[]) => set('activityCodes', next)

  const afmError = afmTouched && form.afm && !isValidAfm(form.afm) ? 'Μη έγκυρος ΑΦΜ (έλεγχος ψηφίου)' : undefined
  const serverErrors = getFieldErrors(save.error)

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    setAfmTouched(true)
    if (!isValidAfm(form.afm)) return
    const request: ClientRequest = {
      ...form,
      activityCodes: codes.filter((c) => c.code.trim()),
      // These inputs are hidden for natural persons; don't keep values typed before switching type.
      ...(form.clientType === 'INDIVIDUAL' && { legalForm: undefined, gemiNumber: undefined }),
    }
    save.mutate({ id: clientId, request }, { onSuccess: onClose })
  }

  return (
    <Modal
      open
      onClose={onClose}
      size="lg"
      title={isEdit ? 'Επεξεργασία πελάτη' : 'Νέος πελάτης'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Άκυρο</Button>
          <Button type="submit" form="client-form" loading={save.isPending}>
            {isEdit ? 'Αποθήκευση' : 'Δημιουργία'}
          </Button>
        </>
      }
    >
      <form id="client-form" onSubmit={onSubmit} className="space-y-5">
        {save.isError && <ErrorAlert>{getErrorMessage(save.error)}</ErrorAlert>}

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Τύπος" required>
            <Select value={form.clientType} onChange={(e) => set('clientType', e.target.value as ClientType)}>
              {Object.entries(clientTypeLabel).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </Select>
          </Field>
          <Field label="Κατηγορία βιβλίων" required>
            <Select value={form.bookCategory} onChange={(e) => set('bookCategory', e.target.value as BookCategory)}>
              {Object.entries(bookCategoryLabel).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </Select>
          </Field>
          <Field label={form.clientType === 'LEGAL_ENTITY' ? 'Εταιρική επωνυμία' : 'Ονοματεπώνυμο'} required error={serverErrors.name} className="sm:col-span-2">
            <Input required maxLength={255} value={form.name} onChange={(e) => set('name', e.target.value)} />
          </Field>
          <Field label="ΑΦΜ" required error={afmError ?? serverErrors.afm}>
            <Input
              required
              inputMode="numeric"
              maxLength={9}
              value={form.afm}
              invalid={!!afmError}
              onBlur={() => setAfmTouched(true)}
              onChange={(e) => set('afm', e.target.value.replace(/\D/g, ''))}
              className="font-mono"
            />
          </Field>
          <Field label="ΔΟΥ" required error={serverErrors.doy}>
            <Input required maxLength={100} value={form.doy} onChange={(e) => set('doy', e.target.value)} placeholder="π.χ. ΔΟΥ Α' Αθηνών" />
          </Field>
          {form.clientType === 'LEGAL_ENTITY' && (
            <>
              <Field label="Νομική μορφή">
                <Input maxLength={30} value={form.legalForm ?? ''} onChange={(e) => set('legalForm', e.target.value)} placeholder="ΑΕ, ΕΠΕ, ΙΚΕ, ΟΕ…" />
              </Field>
              <Field label="Αριθμός ΓΕΜΗ">
                <Input maxLength={20} value={form.gemiNumber ?? ''} onChange={(e) => set('gemiNumber', e.target.value)} />
              </Field>
            </>
          )}
          <Field label="Email" error={serverErrors.email}>
            <Input type="email" value={form.email ?? ''} onChange={(e) => set('email', e.target.value)} />
          </Field>
          <Field label="Τηλέφωνο">
            <Input type="tel" value={form.phone ?? ''} onChange={(e) => set('phone', e.target.value)} />
          </Field>
          <Field label="Διεύθυνση" className="sm:col-span-2">
            <div className="grid gap-2 sm:grid-cols-[1fr_10rem_7rem]">
              <Input placeholder="Οδός, αριθμός" value={form.address?.street ?? ''} onChange={(e) => set('address', { ...form.address, street: e.target.value })} />
              <Input placeholder="Πόλη" value={form.address?.city ?? ''} onChange={(e) => set('address', { ...form.address, city: e.target.value })} />
              <Input placeholder="Τ.Κ." value={form.address?.postalCode ?? ''} onChange={(e) => set('address', { ...form.address, postalCode: e.target.value })} />
            </div>
          </Field>
        </div>

        <fieldset>
          <legend className="mb-2 flex w-full items-center justify-between text-sm font-medium text-slate-700">
            Κωδικοί δραστηριότητας (ΚΑΔ)
            <Button size="sm" variant="ghost" icon={<Plus className="size-3.5" />} onClick={() => setCodes([...codes, { code: '', primary: codes.length === 0 }])}>
              Προσθήκη
            </Button>
          </legend>
          <div className="space-y-2">
            {codes.length === 0 && <p className="text-xs text-slate-500">Δεν έχουν οριστεί ΚΑΔ.</p>}
            {codes.map((kad, i) => (
              <div key={i} className="flex items-center gap-2">
                <Input className="w-40 font-mono" placeholder="69.20.10.01" value={kad.code}
                  onChange={(e) => setCodes(codes.map((c, j) => (j === i ? { ...c, code: e.target.value } : c)))} />
                <Input placeholder="Περιγραφή" value={kad.description ?? ''}
                  onChange={(e) => setCodes(codes.map((c, j) => (j === i ? { ...c, description: e.target.value } : c)))} />
                <label className="flex shrink-0 items-center gap-1 text-xs text-slate-600">
                  <input type="radio" name="primary-kad" checked={kad.primary}
                    onChange={() => setCodes(codes.map((c, j) => ({ ...c, primary: j === i })))} />
                  Κύριος
                </label>
                <button type="button" className="rounded p-1 text-slate-400 hover:text-red-600" aria-label="Αφαίρεση ΚΑΔ"
                  onClick={() => setCodes(codes.filter((_, j) => j !== i))}>
                  <Trash2 className="size-4" />
                </button>
              </div>
            ))}
          </div>
        </fieldset>

        <Field label="Σημειώσεις">
          <Textarea rows={3} value={form.notes ?? ''} onChange={(e) => set('notes', e.target.value)} />
        </Field>
        {isEdit && (
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input type="checkbox" checked={form.active ?? true} onChange={(e) => set('active', e.target.checked)} />
            Ενεργός πελάτης
          </label>
        )}
      </form>
    </Modal>
  )
}
