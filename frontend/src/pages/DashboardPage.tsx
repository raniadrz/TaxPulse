import { Link } from 'react-router-dom'
import { AlertOctagon, CalendarClock, CheckCircle2, ListTodo, Users } from 'lucide-react'
import { PageHeader } from '@/components/layout/PageHeader'
import { StatCard } from '@/components/dashboard/StatCard'
import { StatusBreakdown } from '@/components/dashboard/StatusBreakdown'
import { UpcomingDeadlines } from '@/components/dashboard/UpcomingDeadlines'
import { ClientTable } from '@/components/clients/ClientTable'
import { Card, CardHeader } from '@/components/ui/Card'
import { Spinner } from '@/components/ui/Spinner'
import { ErrorAlert } from '@/components/ui/Alert'
import { useDashboardStats } from '@/hooks/useDashboardStats'
import { useClients } from '@/hooks/useClients'
import { useAuth } from '@/hooks/useAuth'
import { getErrorMessage } from '@/lib/errors'

export default function DashboardPage() {
  const { user } = useAuth()
  const stats = useDashboardStats()
  // Clients needing attention first: sorted by next due date.
  const clients = useClients({ active: true, size: 6, sort: 'name,asc' })

  if (stats.isLoading) return <Spinner />
  if (stats.isError) return <ErrorAlert>{getErrorMessage(stats.error)}</ErrorAlert>
  const s = stats.data!

  return (
    <>
      <PageHeader title={`Καλημέρα, ${user?.fullName.split(' ')[0] ?? ''}`} description="Επισκόπηση γραφείου και επερχόμενων προθεσμιών" />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <StatCard label="Ενεργοί πελάτες" value={s.activeClients} icon={Users} />
        <StatCard label="Ανοιχτές υποχρεώσεις" value={s.openObligations} icon={ListTodo} />
        <StatCard label="Λήγουν σε 7 ημέρες" value={s.dueWithin7Days} icon={CalendarClock} emphasis={s.dueWithin7Days > 0 ? 'warning' : 'default'} />
        <StatCard label="Εκπρόθεσμες" value={s.overdueObligations} icon={AlertOctagon} emphasis={s.overdueObligations > 0 ? 'critical' : 'default'} />
        <StatCard label="Υποβολές μήνα" value={s.submittedThisMonth} icon={CheckCircle2} />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-3">
        <div className="xl:col-span-2">
          <UpcomingDeadlines items={s.upcomingDeadlines} />
        </div>
        <StatusBreakdown counts={s.obligationsByStatus} />
      </div>

      <Card className="mt-6">
        <CardHeader
          title="Πελάτες"
          action={<Link to="/clients" className="text-sm font-medium text-brand-600 hover:underline">Όλοι οι πελάτες</Link>}
        />
        {clients.isLoading ? <Spinner /> : <ClientTable clients={clients.data?.content ?? []} compact />}
      </Card>
    </>
  )
}
