import { useState, type ReactNode } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { ArrowLeft, Building2, CalendarClock, FolderOpen, Pencil, Sparkles, User } from 'lucide-react'
import { ClientFormModal } from '@/components/clients/ClientFormModal'
import { ObligationTable } from '@/components/obligations/ObligationTable'
import { ReminderEmailModal } from '@/components/obligations/ReminderEmailModal'
import { ObligationMessagesModal } from '@/components/messages/ObligationMessagesModal'
import { DocumentTable } from '@/components/documents/DocumentTable'
import { PortalAccountsCard } from '@/components/portal/PortalAccountsCard'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { Card, CardHeader } from '@/components/ui/Card'
import { Spinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/Alert'
import { useClient } from '@/hooks/useClients'
import { useObligations } from '@/hooks/useObligations'
import { useObligationActions } from '@/hooks/useObligationActions'
import { useDocuments } from '@/hooks/useDocuments'
import { useAuth } from '@/hooks/useAuth'
import { documentService } from '@/services/documentService'
import { getErrorMessage } from '@/lib/errors'
import { bookCategoryLabel, clientTypeLabel } from '@/lib/labels'
import { OPEN_AI_ASSISTANT_EVENT } from '@/components/ai/events'

/** 360° view of one client: profile, obligations and documents, with the AI copilot scoped to it. */
export default function ClientDetailPage() {
  const { id = '' } = useParams()
  const { hasRole } = useAuth()
  const canEdit = hasRole('ADMIN', 'ACCOUNTANT')
  const [editing, setEditing] = useState(false)
  const client = useClient(id)
  const obligations = useObligations({ clientId: id, size: 50, sort: 'dueDate,desc' })
  const documents = useDocuments(id)
  const { onStatusChange, statusError, busyId, emailFor, setEmailFor, messagesFor, setMessagesFor } = useObligationActions()
  // ?messages=<obligationId> (from a notification) opens that obligation's conversation.
  const [params, setParams] = useSearchParams()
  const linkedThread = params.get('messages')
  const thread = messagesFor ?? (linkedThread ? obligations.data?.content.find((o) => o.id === linkedThread) ?? null : null)
  const threadId = messagesFor?.id ?? linkedThread
  const closeThread = () => {
    setMessagesFor(null)
    if (linkedThread) setParams({}, { replace: true })
  }

  if (client.isLoading) return <Spinner />
  if (client.isError || !client.data) return <ErrorAlert>{getErrorMessage(client.error, 'Ο πελάτης δεν βρέθηκε.')}</ErrorAlert>
  const c = client.data
  const address = [c.address?.street, c.address?.postalCode, c.address?.city].filter(Boolean).join(', ')

  return (
    <>
      <Link to="/clients" className="mb-4 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700">
        <ArrowLeft className="size-4" /> Πελάτες
      </Link>

      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-start gap-4">
          <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
            {c.clientType === 'LEGAL_ENTITY' ? <Building2 className="size-6" /> : <User className="size-6" />}
          </span>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight text-slate-900">{c.name}</h1>
            <p className="mt-0.5 text-sm text-slate-500">
              ΑΦΜ <span className="font-mono text-slate-700">{c.afm}</span> · {c.doy}
            </p>
            <div className="mt-2 flex flex-wrap gap-1.5">
              <Badge tone="indigo">{clientTypeLabel[c.clientType]}</Badge>
              <Badge>{bookCategoryLabel[c.bookCategory]}</Badge>
              {!c.active && <Badge tone="red">Ανενεργός</Badge>}
            </div>
          </div>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" icon={<Sparkles className="size-4" />}
            onClick={() => window.dispatchEvent(new Event(OPEN_AI_ASSISTANT_EVENT))}>
            Ρώτα το AI
          </Button>
          {canEdit && <Button icon={<Pencil className="size-4" />} onClick={() => setEditing(true)}>Επεξεργασία</Button>}
        </div>
      </div>

      <div className="grid gap-6 xl:grid-cols-3">
        <div className="space-y-6">
          <Card>
            <CardHeader title="Στοιχεία" />
            <dl className="divide-y divide-slate-100 text-sm">
              <Row label="Νομική μορφή">{c.legalForm}</Row>
              <Row label="Διακριτικός τίτλος">{c.tradeName}</Row>
              <Row label="Αρ. ΓΕΜΗ">{c.gemiNumber}</Row>
              <Row label="Email">{c.email && <a className="text-brand-600 hover:underline" href={`mailto:${c.email}`}>{c.email}</a>}</Row>
              <Row label="Τηλέφωνο">{c.phone ?? c.mobile}</Row>
              <Row label="Διεύθυνση">{address}</Row>
              <Row label="Λογιστής">{c.assignedAccountant?.fullName}</Row>
            </dl>
            {c.notes && <p className="whitespace-pre-wrap border-t border-slate-100 px-5 py-3 text-sm text-slate-600">{c.notes}</p>}
          </Card>

          <Card>
            <CardHeader title="Κωδικοί δραστηριότητας (ΚΑΔ)" />
            {c.activityCodes.length === 0 ? (
              <p className="px-5 py-4 text-sm text-slate-500">Δεν έχουν οριστεί ΚΑΔ.</p>
            ) : (
              <ul className="divide-y divide-slate-100 text-sm">
                {c.activityCodes.map((k) => (
                  <li key={k.code} className="flex items-center gap-2 px-5 py-2.5">
                    <span className="font-mono text-slate-900">{k.code}</span>
                    <span className="truncate text-slate-500">{k.description}</span>
                    {k.primary && <Badge tone="indigo" className="ml-auto">Κύριος</Badge>}
                  </li>
                ))}
              </ul>
            )}
          </Card>

          {c.representatives.length > 0 && (
            <Card>
              <CardHeader title="Εκπρόσωποι" />
              <ul className="divide-y divide-slate-100 text-sm">
                {c.representatives.map((r) => (
                  <li key={r.id ?? r.fullName} className="px-5 py-2.5">
                    <div className="font-medium text-slate-900">{r.fullName}</div>
                    <div className="text-xs text-slate-500">{[r.role, r.email, r.phone].filter(Boolean).join(' · ')}</div>
                  </li>
                ))}
              </ul>
            </Card>
          )}

          <PortalAccountsCard clientId={c.id} canEdit={canEdit} />
        </div>

        <div className="space-y-6 xl:col-span-2">
          <Card>
            <CardHeader title="Υποχρεώσεις" description={obligations.data ? `${obligations.data.totalElements} συνολικά` : undefined}
              action={<Link to="/obligations" className="text-sm font-medium text-brand-600 hover:underline">Ημερολόγιο</Link>} />
            {statusError && <div className="p-4"><ErrorAlert>{getErrorMessage(statusError)}</ErrorAlert></div>}
            {obligations.isLoading && <Spinner />}
            {obligations.data?.content.length === 0 && (
              <EmptyState icon={<CalendarClock className="size-10" />} title="Καμία υποχρέωση" description="Δεν έχουν καταχωριστεί υποχρεώσεις για τον πελάτη." />
            )}
            {obligations.data && obligations.data.content.length > 0 && (
              <ObligationTable obligations={obligations.data.content} onStatusChange={onStatusChange}
                onDraftEmail={setEmailFor} onMessages={setMessagesFor} busyId={busyId} hideClient />
            )}
          </Card>

          <Card>
            <CardHeader title="Έγγραφα" description={documents.data ? `${documents.data.totalElements} αρχεία` : undefined}
              action={<Link to={`/documents?clientId=${c.id}`} className="text-sm font-medium text-brand-600 hover:underline">Διαχείριση</Link>} />
            {documents.isLoading && <Spinner />}
            {documents.data?.content.length === 0 && (
              <EmptyState icon={<FolderOpen className="size-10" />} title="Δεν υπάρχουν έγγραφα" />
            )}
            {documents.data && documents.data.content.length > 0 && (
              <DocumentTable documents={documents.data.content.slice(0, 5)} onDownload={(d) => void documentService.download(d)} />
            )}
          </Card>
        </div>
      </div>

      {editing && <ClientFormModal clientId={c.id} onClose={() => setEditing(false)} />}
      <ReminderEmailModal obligation={emailFor} onClose={() => setEmailFor(null)} />
      {threadId && (
        <ObligationMessagesModal key={threadId} scope="staff" obligationId={threadId} title={thread?.title} onClose={closeThread} />
      )}
    </>
  )
}

function Row({ label, children }: { label: string; children?: ReactNode }) {
  return (
    <div className="grid grid-cols-[8rem_1fr] gap-2 px-5 py-2.5">
      <dt className="text-slate-500">{label}</dt>
      <dd className="min-w-0 break-words text-slate-900">{children || '—'}</dd>
    </div>
  )
}
