import type { ReactNode } from 'react'
import { Mail } from 'lucide-react'
import { PageHeader } from '@/components/layout/PageHeader'
import { Card, CardHeader } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { ChangePasswordCard } from '@/components/portal/ChangePasswordCard'
import { Spinner } from '@/components/ui/Spinner'
import { ErrorAlert } from '@/components/ui/Alert'
import { usePortalProfile } from '@/hooks/usePortal'
import { useAuth } from '@/hooks/useAuth'
import { getErrorMessage } from '@/lib/errors'
import { clientTypeLabel } from '@/lib/labels'

export default function PortalProfilePage() {
  const { user } = useAuth()
  const profile = usePortalProfile()

  if (profile.isLoading) return <Spinner />
  if (profile.isError || !profile.data) return <ErrorAlert>{getErrorMessage(profile.error)}</ErrorAlert>
  const p = profile.data
  const address = [p.address?.street, p.address?.postalCode, p.address?.city].filter(Boolean).join(', ')

  return (
    <>
      <PageHeader title="Τα στοιχεία μου" description="Για οποιαδήποτε αλλαγή στα στοιχεία σας, ενημερώστε το γραφείο." />
      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader title={p.name} action={<Badge tone="indigo">{clientTypeLabel[p.clientType]}</Badge>} />
          <dl className="divide-y divide-slate-100 text-sm">
            <Row label="ΑΦΜ"><span className="font-mono">{p.afm}</span></Row>
            <Row label="ΔΟΥ">{p.doy}</Row>
            <Row label="Νομική μορφή">{p.legalForm}</Row>
            <Row label="Email">{p.email}</Row>
            <Row label="Τηλέφωνο">{p.phone}</Row>
            <Row label="Διεύθυνση">{address}</Row>
          </dl>
        </Card>
        <div className="space-y-6">
          <Card>
            <CardHeader title="Ο λογαριασμός μου" />
            <dl className="divide-y divide-slate-100 text-sm">
              <Row label="Όνομα">{user?.fullName}</Row>
              <Row label="Email σύνδεσης">{user?.email}</Row>
            </dl>
          </Card>
          <ChangePasswordCard />
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
      </div>
    </>
  )
}

function Row({ label, children }: { label: string; children?: ReactNode }) {
  return (
    <div className="grid grid-cols-[9rem_1fr] gap-2 px-5 py-2.5">
      <dt className="text-slate-500">{label}</dt>
      <dd className="min-w-0 break-words text-slate-900">{children || '—'}</dd>
    </div>
  )
}
