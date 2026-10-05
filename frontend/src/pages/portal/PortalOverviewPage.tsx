import { Link } from 'react-router-dom'
import { AlertOctagon, CalendarClock, CheckCircle2, FolderOpen, ListTodo, Mail } from 'lucide-react'
import { PageHeader } from '@/components/layout/PageHeader'
import { StatCard } from '@/components/dashboard/StatCard'
import { DueDateCell } from '@/components/obligations/DueDateCell'
import { ObligationStatusBadge } from '@/components/obligations/ObligationStatusBadge'
import { DocumentTable } from '@/components/documents/DocumentTable'
import { Card, CardHeader } from '@/components/ui/Card'
import { Spinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/Alert'
import { usePortalDocuments, usePortalObligations, usePortalProfile } from '@/hooks/usePortal'
import { useAuth } from '@/hooks/useAuth'
import { portalService } from '@/services/portalService'
import { getErrorMessage } from '@/lib/errors'
import { formatDate, formatRelativeDays } from '@/lib/format'
import { OPEN_STATUSES } from './portalStatuses'

/** The client's home: what needs attention now, at a glance. */
export default function PortalOverviewPage() {
  const { user } = useAuth()
  const profile = usePortalProfile()
  const upcoming = usePortalObligations(OPEN_STATUSES, 'dueDate,asc')
  const documents = usePortalDocuments(0)

  if (profile.isLoading) return <Spinner />
  if (profile.isError || !profile.data) return <ErrorAlert>{getErrorMessage(profile.error)}</ErrorAlert>
  const p = profile.data
  const next = upcoming.data?.content.find((o) => o.status !== 'OVERDUE')

  return (
    <>
      <PageHeader title={`Καλώς ήρθατε, ${user?.fullName.split(' ')[0] ?? ''}`} description={`${p.name} · ΑΦΜ ${p.afm}`} />

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3 sm:gap-4">
        <StatCard label="Σε εκκρεμότητα" value={p.openObligations} icon={ListTodo} />
        <StatCard label="Εκπρόθεσμες" value={p.overdueObligations} icon={AlertOctagon}
          emphasis={p.overdueObligations > 0 ? 'critical' : 'default'}
          hint={p.overdueObligations > 0 ? 'Επικοινωνήστε με το γραφείο' : undefined} />
        <StatCard label="Επόμενη προθεσμία" value={p.nextDueDate ? formatDate(p.nextDueDate) : '—'} icon={CalendarClock}
          emphasis={next && next.daysUntilDue <= 7 ? 'warning' : 'default'}
          hint={next ? formatRelativeDays(next.daysUntilDue) : 'Καμία ανοιχτή υποχρέωση'} />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader title="Επόμενες προθεσμίες"
            action={<Link to="/portal/obligations" className="text-sm font-medium text-brand-600 hover:underline">Όλες οι υποχρεώσεις</Link>} />
          {upcoming.isLoading && <Spinner />}
          {upcoming.data?.content.length === 0 && (
            <EmptyState icon={<CheckCircle2 className="size-10" />} title="Δεν υπάρχουν εκκρεμότητες"
              description="Θα ειδοποιηθείτε όταν χρειαστεί κάτι από εσάς." />
          )}
          <ul className="divide-y divide-slate-100">
            {upcoming.data?.content.slice(0, 5).map((o) => (
              <li key={o.id} className="flex items-center gap-4 px-5 py-3">
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-medium text-slate-900">{o.title}</div>
                  <div className="text-xs text-slate-500">{o.obligationTypeLabel}</div>
                </div>
                <DueDateCell dueDate={o.dueDate} daysUntilDue={o.daysUntilDue} status={o.status} />
                <ObligationStatusBadge status={o.status} />
              </li>
            ))}
          </ul>
        </Card>

        <Card>
          <CardHeader title="Ο λογιστής σας" />
          {p.accountant ? (
            <div className="px-5 pb-5 text-sm">
              <div className="font-medium text-slate-900">{p.accountant.fullName}</div>
              <a href={`mailto:${p.accountant.email}`} className="mt-1 inline-flex items-center gap-1.5 text-brand-600 hover:underline">
                <Mail className="size-4" aria-hidden />{p.accountant.email}
              </a>
            </div>
          ) : (
            <p className="px-5 pb-5 text-sm text-slate-500">Δεν έχει οριστεί υπεύθυνος λογιστής.</p>
          )}
        </Card>
      </div>

      <Card className="mt-6">
        <CardHeader title="Πρόσφατα έγγραφα"
          action={<Link to="/portal/documents" className="text-sm font-medium text-brand-600 hover:underline">Όλα τα έγγραφα</Link>} />
        {documents.isLoading && <Spinner />}
        {documents.data?.content.length === 0 && (
          <EmptyState icon={<FolderOpen className="size-10" />} title="Δεν έχετε ανεβάσει έγγραφα"
            action={<Link to="/portal/documents" className="text-sm font-medium text-brand-600 hover:underline">Ανέβασμα εγγράφων</Link>} />
        )}
        {documents.data && documents.data.content.length > 0 && (
          <DocumentTable documents={documents.data.content.slice(0, 5)} onDownload={(d) => void portalService.download(d)} />
        )}
      </Card>
    </>
  )
}
