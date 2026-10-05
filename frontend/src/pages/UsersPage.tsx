import { useState } from 'react'
import { Link } from 'react-router-dom'
import { KeyRound, Pencil, Plus, UserCog } from 'lucide-react'
import { PageHeader } from '@/components/layout/PageHeader'
import { UserFormModal } from '@/components/users/UserFormModal'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Badge, type BadgeTone } from '@/components/ui/Badge'
import { Spinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/Alert'
import { useUsers } from '@/hooks/useUsers'
import { useAllPortalAccounts } from '@/hooks/usePortal'
import { useAuth } from '@/hooks/useAuth'
import { getErrorMessage } from '@/lib/errors'
import { formatDateTime } from '@/lib/format'
import { cn } from '@/lib/cn'
import { roleLabel } from '@/lib/labels'
import type { Role, User } from '@/types/api'

const roleTone: Record<Role, BadgeTone> = { ADMIN: 'indigo', ACCOUNTANT: 'blue', ASSISTANT: 'gray', CLIENT: 'green' }

const TABS = [['staff', 'Προσωπικό'], ['clients', 'Πελάτες (portal)']] as const

/** Account administration (ADMIN only; the route and API both enforce it): staff, and client portal logins. */
export default function UsersPage() {
  const { user: me } = useAuth()
  const [tab, setTab] = useState<'staff' | 'clients'>('staff')
  const users = useUsers()
  const portalAccounts = useAllPortalAccounts(tab === 'clients')
  const [editing, setEditing] = useState<{ open: boolean; user?: User }>({ open: false })

  return (
    <>
      <PageHeader
        title="Χρήστες"
        description={tab === 'staff'
          ? 'Λογαριασμοί λογιστών και προσωπικού του γραφείου'
          : 'Λογαριασμοί πελατών για το portal. Δημιουργούνται από την καρτέλα κάθε πελάτη («Πρόσβαση στο portal»).'}
        actions={tab === 'staff'
          ? <Button icon={<Plus className="size-4" />} onClick={() => setEditing({ open: true })}>Νέος χρήστης</Button>
          : undefined}
      />
      <div className="mb-4 inline-flex rounded-lg bg-slate-200/60 p-0.5 text-sm" role="tablist">
        {TABS.map(([key, label]) => (
          <button key={key} type="button" role="tab" aria-selected={tab === key} onClick={() => setTab(key)}
            className={cn('rounded-md px-3 py-1.5 font-medium', tab === key ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900')}>
            {label}
          </button>
        ))}
      </div>
      {tab === 'clients' ? <PortalAccountsTable query={portalAccounts} /> : <Card>
        {users.isLoading && <Spinner />}
        {users.isError && <div className="p-4"><ErrorAlert>{getErrorMessage(users.error)}</ErrorAlert></div>}
        {users.data?.length === 0 && <EmptyState icon={<UserCog className="size-10" />} title="Δεν υπάρχουν χρήστες" />}
        {users.data && users.data.length > 0 && (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                <tr>
                  <th scope="col" className="px-5 py-3">Χρήστης</th>
                  <th scope="col" className="px-3 py-3">Ρόλος</th>
                  <th scope="col" className="px-3 py-3">Κατάσταση</th>
                  <th scope="col" className="px-3 py-3">Τελευταία σύνδεση</th>
                  <th scope="col" className="px-5 py-3"><span className="sr-only">Ενέργειες</span></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {users.data.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50">
                    <td className="px-5 py-3">
                      <div className="font-medium text-slate-900">
                        {u.fullName}
                        {u.id === me?.id && <span className="ml-2 text-xs font-normal text-slate-500">(εσείς)</span>}
                      </div>
                      <div className="text-xs text-slate-500">{u.email}</div>
                    </td>
                    <td className="px-3 py-3"><Badge tone={roleTone[u.role]}>{roleLabel[u.role]}</Badge></td>
                    <td className="px-3 py-3">{u.active ? <Badge tone="green">Ενεργός</Badge> : <Badge>Ανενεργός</Badge>}</td>
                    <td className="whitespace-nowrap px-3 py-3 text-slate-600">{formatDateTime(u.lastLoginAt)}</td>
                    <td className="px-5 py-3 text-right">
                      <button type="button" onClick={() => setEditing({ open: true, user: u })}
                        className="rounded-md p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-700" aria-label={`Επεξεργασία ${u.fullName}`}>
                        <Pencil className="size-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>}
      {editing.open && (
        <UserFormModal key={editing.user?.id ?? 'new'} user={editing.user} isSelf={editing.user?.id === me?.id}
          onClose={() => setEditing({ open: false })} />
      )}
    </>
  )
}

function PortalAccountsTable({ query }: { query: ReturnType<typeof useAllPortalAccounts> }) {
  return (
    <Card>
      {query.isLoading && <Spinner />}
      {query.isError && <div className="p-4"><ErrorAlert>{getErrorMessage(query.error)}</ErrorAlert></div>}
      {query.data?.length === 0 && (
        <EmptyState icon={<KeyRound className="size-10" />} title="Κανένας πελάτης δεν έχει πρόσβαση ακόμη"
          description="Ανοίξτε την καρτέλα ενός πελάτη και πατήστε «Λογαριασμός» στην κάρτα «Πρόσβαση στο portal»." />
      )}
      {query.data && query.data.length > 0 && (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
              <tr>
                <th scope="col" className="px-5 py-3">Χρήστης</th>
                <th scope="col" className="px-3 py-3">Πελάτης</th>
                <th scope="col" className="px-3 py-3">Κατάσταση</th>
                <th scope="col" className="px-3 py-3">Τελευταία σύνδεση</th>
                <th scope="col" className="px-5 py-3"><span className="sr-only">Ενέργειες</span></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {query.data.map((a) => (
                <tr key={a.id} className="hover:bg-slate-50">
                  <td className="px-5 py-3">
                    <div className="font-medium text-slate-900">{a.fullName}</div>
                    <div className="text-xs text-slate-500">{a.email}</div>
                  </td>
                  <td className="px-3 py-3">
                    <Link to={`/clients/${a.clientId}`} className="text-slate-900 hover:text-brand-600 hover:underline">{a.clientName ?? '—'}</Link>
                  </td>
                  <td className="px-3 py-3">
                    {!a.active ? <Badge>Ανενεργός</Badge>
                      : !a.clientActive ? <Badge tone="amber">Ανενεργός πελάτης</Badge>
                      : <Badge tone="green">Ενεργός</Badge>}
                  </td>
                  <td className="whitespace-nowrap px-3 py-3 text-slate-600">{a.lastLoginAt ? formatDateTime(a.lastLoginAt) : 'Δεν έχει συνδεθεί'}</td>
                  <td className="px-5 py-3 text-right">
                    <Link to={`/clients/${a.clientId}`} className="text-sm font-medium text-brand-600 hover:underline">Διαχείριση</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  )
}
