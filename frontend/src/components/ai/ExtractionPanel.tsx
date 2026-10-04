import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { AlertTriangle, CheckCircle2, ScanText, XCircle } from 'lucide-react'
import { aiService } from '@/services/aiService'
import { Button } from '@/components/ui/Button'
import { Textarea } from '@/components/ui/FormField'
import { ErrorAlert } from '@/components/ui/Alert'
import { getErrorMessage } from '@/lib/errors'
import { formatCurrency, formatDate } from '@/lib/format'
import { obligationTypeLabel } from '@/lib/labels'

/** AI use case 1: paste unstructured notes, get validated structured records back. */
export function ExtractionPanel() {
  const [text, setText] = useState('')
  const extract = useMutation({ mutationFn: (t: string) => aiService.extract(t) })

  return (
    <div className="flex h-full flex-col gap-3 overflow-y-auto p-4">
      <Textarea
        rows={6}
        value={text}
        maxLength={20000}
        onChange={(e) => setText(e.target.value)}
        placeholder="Επικολλήστε σημειώσεις, email ή κείμενο παραστατικού… π.χ. «Τιμολόγιο ΑΛΦΑ ΑΕ, ΑΦΜ 094014201, 1.240,00 € με ημερομηνία 15/10/2026»"
      />
      <Button icon={<ScanText className="size-4" />} loading={extract.isPending} disabled={!text.trim()} onClick={() => extract.mutate(text)}>
        Εξαγωγή δεδομένων
      </Button>

      {extract.isError && <ErrorAlert>{getErrorMessage(extract.error)}</ErrorAlert>}

      {extract.data && (
        <div className="space-y-2">
          {extract.data.warnings.map((w) => (
            <p key={w} className="flex items-start gap-1.5 text-xs text-amber-800">
              <AlertTriangle className="mt-0.5 size-3.5 shrink-0" aria-hidden /> {w}
            </p>
          ))}
          {extract.data.records.length === 0 && <p className="text-sm text-slate-500">Δεν εντοπίστηκαν οικονομικά στοιχεία.</p>}
          {extract.data.records.map((r, i) => (
            <dl key={i} className="grid grid-cols-[6rem_1fr] gap-x-2 gap-y-1 rounded-lg border border-slate-200 p-3 text-sm">
              <dt className="text-slate-500">ΑΦΜ</dt>
              <dd className="flex items-center gap-1 font-mono">
                {r.afm ?? '—'}
                {r.afm &&
                  (r.afmValid ? (
                    <CheckCircle2 className="size-3.5 text-emerald-600" aria-label="έγκυρος" />
                  ) : (
                    <XCircle className="size-3.5 text-red-600" aria-label="μη έγκυρος" />
                  ))}
              </dd>
              <dt className="text-slate-500">Πελάτης</dt>
              <dd>{r.matchedClient?.name ?? r.partyName ?? '—'}{r.matchedClient && <span className="ml-1 text-xs text-emerald-700">(υπάρχει στο CRM)</span>}</dd>
              <dt className="text-slate-500">Ποσό</dt>
              <dd className="tabular-nums">{r.amount != null ? formatCurrency(r.amount) : '—'}</dd>
              <dt className="text-slate-500">Ημερομηνία</dt>
              <dd>{formatDate(r.date)}</dd>
              {r.obligationType && (
                <>
                  <dt className="text-slate-500">Υποχρέωση</dt>
                  <dd>{obligationTypeLabel[r.obligationType]}</dd>
                </>
              )}
              {r.description && (
                <>
                  <dt className="text-slate-500">Περιγραφή</dt>
                  <dd>{r.description}</dd>
                </>
              )}
            </dl>
          ))}
        </div>
      )}
    </div>
  )
}
