import { useState, type FormEvent } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { Field, Input, Select, Textarea } from '@/components/ui/FormField'
import { ErrorAlert } from '@/components/ui/Alert'
import { ClientPicker } from '@/components/clients/ClientPicker'
import { useSaveObligation } from '@/hooks/useObligations'
import { getErrorMessage, getFieldErrors } from '@/lib/errors'
import { obligationTypeLabel } from '@/lib/labels'
import type { ObligationRequest, ObligationType } from '@/types/api'

const initial = (): ObligationRequest => ({
  clientId: '',
  obligationType: 'VAT',
  title: '',
  dueDate: new Date().toISOString().slice(0, 10),
})

/**
 * Creates a new obligation; the client is picked with the searchable ClientPicker.
 * Mounted only while open, so each opening starts with a clean form.
 */
export function ObligationFormModal({ onClose }: { onClose: () => void }) {
  const save = useSaveObligation()
  const [form, setForm] = useState<ObligationRequest>(initial)

  const set = <K extends keyof ObligationRequest>(key: K, value: ObligationRequest[K]) => setForm((f) => ({ ...f, [key]: value }))
  const errors = getFieldErrors(save.error)

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    save.mutate({ request: { ...form, amount: form.amount || undefined } }, { onSuccess: onClose })
  }

  return (
    <Modal
      open
      onClose={onClose}
      title="Νέα φορολογική υποχρέωση"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Άκυρο</Button>
          <Button type="submit" form="obligation-form" loading={save.isPending} disabled={!form.clientId}>Δημιουργία</Button>
        </>
      }
    >
      <form id="obligation-form" onSubmit={onSubmit} className="space-y-4">
        {save.isError && <ErrorAlert>{getErrorMessage(save.error)}</ErrorAlert>}
        <Field label="Πελάτης" required error={errors.clientId}>
          <ClientPicker required value={form.clientId} onChange={(id) => set('clientId', id)} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Τύπος" required>
            <Select value={form.obligationType} onChange={(e) => set('obligationType', e.target.value as ObligationType)}>
              {Object.entries(obligationTypeLabel).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </Select>
          </Field>
          <Field label="Προθεσμία" required error={errors.dueDate}>
            <Input type="date" required value={form.dueDate} onChange={(e) => set('dueDate', e.target.value)} />
          </Field>
        </div>
        <Field label="Τίτλος" required error={errors.title}>
          <Input required maxLength={255} placeholder="π.χ. ΦΠΑ Γ' τριμήνου 2026" value={form.title} onChange={(e) => set('title', e.target.value)} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="Περίοδος από">
            <Input type="date" value={form.periodStart ?? ''} onChange={(e) => set('periodStart', e.target.value || undefined)} />
          </Field>
          <Field label="Περίοδος έως" error={errors.periodValid}>
            <Input type="date" value={form.periodEnd ?? ''} onChange={(e) => set('periodEnd', e.target.value || undefined)} />
          </Field>
          <Field label="Ποσό (€)">
            <Input type="number" step="0.01" min="0" value={form.amount ?? ''} onChange={(e) => set('amount', e.target.value ? Number(e.target.value) : undefined)} />
          </Field>
        </div>
        <Field label="Σημειώσεις">
          <Textarea rows={2} value={form.notes ?? ''} onChange={(e) => set('notes', e.target.value)} />
        </Field>
      </form>
    </Modal>
  )
}
