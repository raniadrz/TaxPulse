import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { FolderOpen } from 'lucide-react'
import { PageHeader } from '@/components/layout/PageHeader'
import { ClientPicker } from '@/components/clients/ClientPicker'
import { DocumentUploadZone } from '@/components/documents/DocumentUploadZone'
import { DocumentTable } from '@/components/documents/DocumentTable'
import { AskDocumentsPanel } from '@/components/documents/AskDocumentsPanel'
import { Card, CardHeader } from '@/components/ui/Card'
import { Pagination } from '@/components/ui/Pagination'
import { Spinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/Alert'
import { useDeleteDocument, useDocuments, useReindexDocument } from '@/hooks/useDocuments'
import { useAuth } from '@/hooks/useAuth'
import { documentService } from '@/services/documentService'
import { getErrorMessage } from '@/lib/errors'
import type { DocumentInfo } from '@/types/api'

export default function DocumentsPage() {
  const { hasRole } = useAuth()
  const canManage = hasRole('ADMIN', 'ACCOUNTANT')
  const [params, setParams] = useSearchParams()
  const clientId = params.get('clientId') ?? ''
  const [page, setPage] = useState(0)
  const [actionError, setActionError] = useState<string | null>(null)

  const documents = useDocuments(clientId || undefined, page)
  const reindex = useReindexDocument()
  const remove = useDeleteDocument()

  const selectClient = (id: string) => {
    setPage(0)
    setActionError(null)
    setParams(id ? { clientId: id } : {}, { replace: true })
  }

  const run = (action: () => Promise<unknown>) => {
    setActionError(null)
    action().catch((err) => setActionError(getErrorMessage(err)))
  }

  const onDelete = (d: DocumentInfo) => {
    if (window.confirm(`Διαγραφή του «${d.originalFilename}»; Η ενέργεια δεν αναιρείται.`)) {
      run(() => remove.mutateAsync(d.id))
    }
  }

  const indexedCount = documents.data?.content.filter((d) => d.ingestionStatus === 'INDEXED').length ?? 0

  return (
    <>
      <PageHeader title="Έγγραφα" description="Αρχείο εγγράφων ανά πελάτη και αναζήτηση με τον AI Copilot" />

      <Card className="mb-6 p-5">
        <div className="max-w-xl">
          <ClientPicker value={clientId} onChange={selectClient} />
        </div>
      </Card>

      {!clientId ? (
        <Card>
          <EmptyState icon={<FolderOpen className="size-10" />} title="Επιλέξτε πελάτη" description="Τα έγγραφα οργανώνονται ανά πελάτη. Επιλέξτε έναν για να δείτε ή να ανεβάσετε αρχεία." />
        </Card>
      ) : (
        <div className="grid gap-6 xl:grid-cols-3">
          <div className="space-y-6 xl:col-span-2">
            <Card className="p-5">
              <DocumentUploadZone clientId={clientId} />
            </Card>
            <Card>
              <CardHeader title="Αρχεία πελάτη" description={documents.data ? `${documents.data.totalElements} έγγραφα` : undefined} />
              {actionError && <div className="p-4"><ErrorAlert>{actionError}</ErrorAlert></div>}
              {documents.isLoading && <Spinner />}
              {documents.isError && <div className="p-4"><ErrorAlert>{getErrorMessage(documents.error)}</ErrorAlert></div>}
              {documents.data?.content.length === 0 && (
                <EmptyState icon={<FolderOpen className="size-10" />} title="Δεν υπάρχουν έγγραφα" description="Ανεβάστε το πρώτο αρχείο για αυτόν τον πελάτη." />
              )}
              {documents.data && documents.data.content.length > 0 && (
                <>
                  <DocumentTable
                    documents={documents.data.content}
                    onDownload={(d) => run(() => documentService.download(d))}
                    onReindex={canManage ? (d) => run(() => reindex.mutateAsync(d.id)) : undefined}
                    onDelete={canManage ? onDelete : undefined}
                  />
                  <Pagination page={documents.data} onPageChange={setPage} />
                </>
              )}
            </Card>
          </div>
          <AskDocumentsPanel clientId={clientId} indexedCount={indexedCount} />
        </div>
      )}
    </>
  )
}
