import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Check, Copy, Mail, Sparkles } from 'lucide-react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { Field, Input, Select, Textarea } from '@/components/ui/FormField'
import { ErrorAlert } from '@/components/ui/Alert'
import { aiService } from '@/services/aiService'
import { getErrorMessage } from '@/lib/errors'
import { copyText } from '@/lib/clipboard'
import type { EmailTone, Obligation } from '@/types/api'

/**
 * AI use case 2: drafts a reminder e-mail with the local LLM. The accountant reviews/edits the
 * draft and sends it from their own mail client - nothing is sent automatically.
 */
export function ReminderEmailModal({ obligation, onClose }: { obligation: Obligation | null; onClose: () => void }) {
  return (
    <Modal open={!!obligation} onClose={onClose} size="lg" title="Email υπενθύμισης (AI)">
      {/* Keyed by obligation so switching rows always starts a fresh draft. */}
      {obligation && <ReminderEmailDraft key={obligation.id} obligation={obligation} />}
    </Modal>
  )
}

function ReminderEmailDraft({ obligation }: { obligation: Obligation }) {
  const [tone, setTone] = useState<EmailTone>('FORMAL')
  const [instructions, setInstructions] = useState('')
  const [subject, setSubject] = useState('')
  const [body, setBody] = useState('')
  const [copied, setCopied] = useState(false)

  const draft = useMutation({
    mutationFn: () => aiService.draftReminderEmail(obligation.id, tone, instructions || undefined),
    onSuccess: (d) => {
      setSubject(d.subject)
      setBody(d.body)
    },
  })

  const copy = async () => {
    try {
      await copyText(`${subject}\n\n${body}`)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch (err) {
      window.alert(getErrorMessage(err))
    }
  }

  const mailto = `mailto:${draft.data?.recipientEmail ?? ''}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`

  return (
    <div className="space-y-4">
      <p className="text-sm text-slate-600">
        <span className="font-medium text-slate-900">{obligation.client.name}</span> · {obligation.obligationTypeLabel} · {obligation.title}
      </p>
      <div className="grid gap-3 sm:grid-cols-[12rem_1fr_auto] sm:items-end">
        <Field label="Ύφος">
          <Select value={tone} onChange={(e) => setTone(e.target.value as EmailTone)}>
            <option value="FORMAL">Επίσημο</option>
            <option value="FRIENDLY">Φιλικό</option>
            <option value="URGENT">Επείγον</option>
          </Select>
        </Field>
        <Field label="Επιπλέον οδηγίες (προαιρετικά)">
          <Input value={instructions} maxLength={1000} onChange={(e) => setInstructions(e.target.value)} placeholder="π.χ. ζήτησε και το μισθωτήριο" />
        </Field>
        <Button icon={<Sparkles className="size-4" />} loading={draft.isPending} onClick={() => draft.mutate()}>
          {body ? 'Νέα πρόταση' : 'Σύνταξη'}
        </Button>
      </div>
      {draft.isPending && <p className="text-xs text-slate-500">Το τοπικό μοντέλο συντάσσει το email… (μπορεί να διαρκέσει λίγα δευτερόλεπτα)</p>}
      {draft.isError && <ErrorAlert>{getErrorMessage(draft.error)}</ErrorAlert>}
      {body && (
        <>
          <Field label={`Προς: ${draft.data?.recipientName ?? ''} ${draft.data?.recipientEmail ? `<${draft.data.recipientEmail}>` : ''}`}>
            <Input value={subject} onChange={(e) => setSubject(e.target.value)} />
          </Field>
          <Textarea rows={12} value={body} onChange={(e) => setBody(e.target.value)} />
          <div className="flex justify-end gap-2">
            <Button variant="secondary" icon={copied ? <Check className="size-4" /> : <Copy className="size-4" />} onClick={copy}>
              {copied ? 'Αντιγράφηκε' : 'Αντιγραφή'}
            </Button>
            <a href={mailto} className="inline-flex items-center gap-2 rounded-lg bg-brand-600 px-3.5 py-2 text-sm font-medium text-white shadow-sm hover:bg-brand-700">
              <Mail className="size-4" /> Άνοιγμα στο email
            </a>
          </div>
        </>
      )}
    </div>
  )
}
