import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Plus, Search, Users } from 'lucide-react'
import { PageHeader } from '@/components/layout/PageHeader'
import { ClientTable } from '@/components/clients/ClientTable'
import { ClientFormModal } from '@/components/clients/ClientFormModal'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Input, Select } from '@/components/ui/FormField'
import { Pagination } from '@/components/ui/Pagination'
import { Spinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/Alert'
import { useClients } from '@/hooks/useClients'
import { useDebounce } from '@/hooks/useDebounce'
import { useAuth } from '@/hooks/useAuth'
import { getErrorMessage } from '@/lib/errors'
import { bookCategoryLabel, clientTypeLabel } from '@/lib/labels'
import type { BookCategory, ClientType, UUID } from '@/types/api'

export default function ClientsPage() {
  const { hasRole } = useAuth()
  const canEdit = hasRole('ADMIN', 'ACCOUNTANT')
  const [params, setParams] = useSearchParams()
  const [query, setQuery] = useState(params.get('q') ?? '')
  const [type, setType] = useState<ClientType | ''>('')
  const [books, setBooks] = useState<BookCategory | ''>('')
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<{ open: boolean; id?: UUID }>({ open: false })
  const q = useDebounce(query.trim())

  const clients = useClients({
    q: q || undefined,
    type: type || undefined,
    bookCategory: books || undefined,
    page,
    size: 20,
    sort: 'name,asc',
  })

  const onQueryChange = (value: string) => {
    setQuery(value)
    setPage(0)
    setParams(value ? { q: value } : {}, { replace: true })
  }

  return (
    <>
      <PageHeader
        title="Πελάτες"
        description="Φυσικά και νομικά πρόσωπα του γραφείου"
        actions={canEdit && <Button icon={<Plus className="size-4" />} onClick={() => setEditing({ open: true })}>Νέος πελάτης</Button>}
      />

      <Card>
        <div className="flex flex-col gap-3 border-b border-slate-100 p-4 sm:flex-row">
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
            <Input value={query} onChange={(e) => onQueryChange(e.target.value)} placeholder="ΑΦΜ ή επωνυμία…" className="pl-9" aria-label="Αναζήτηση" />
          </div>
          <Select className="sm:w-48" value={type} onChange={(e) => { setType(e.target.value as ClientType | ''); setPage(0) }} aria-label="Τύπος">
            <option value="">Όλοι οι τύποι</option>
            {Object.entries(clientTypeLabel).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </Select>
          <Select className="sm:w-48" value={books} onChange={(e) => { setBooks(e.target.value as BookCategory | ''); setPage(0) }} aria-label="Κατηγορία βιβλίων">
            <option value="">Όλες οι κατηγορίες</option>
            {Object.entries(bookCategoryLabel).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </Select>
        </div>

        {clients.isLoading && <Spinner />}
        {clients.isError && <div className="p-4"><ErrorAlert>{getErrorMessage(clients.error)}</ErrorAlert></div>}
        {clients.data && clients.data.content.length === 0 && (
          <EmptyState icon={<Users className="size-10" />} title="Δεν βρέθηκαν πελάτες" description={q ? 'Δοκιμάστε άλλο ΑΦΜ ή επωνυμία.' : 'Προσθέστε τον πρώτο σας πελάτη.'} />
        )}
        {clients.data && clients.data.content.length > 0 && (
          <>
            <ClientTable clients={clients.data.content} onEdit={canEdit ? (c) => setEditing({ open: true, id: c.id }) : undefined} />
            <Pagination page={clients.data} onPageChange={setPage} />
          </>
        )}
      </Card>

      {editing.open && (
        <ClientFormModal key={editing.id ?? 'new'} clientId={editing.id} onClose={() => setEditing({ open: false })} />
      )}
    </>
  )
}
