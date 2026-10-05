import { useState } from 'react'
import { FolderOpen } from 'lucide-react'
import { PageHeader } from '@/components/layout/PageHeader'
import { DocumentUploadZone } from '@/components/documents/DocumentUploadZone'
import { DocumentTable } from '@/components/documents/DocumentTable'
import { Card, CardHeader } from '@/components/ui/Card'
import { Pagination } from '@/components/ui/Pagination'
import { Spinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/Alert'
import { usePortalDocuments, usePortalUpload } from '@/hooks/usePortal'
import { portalService } from '@/services/portalService'
import { getErrorMessage } from '@/lib/errors'

export default function PortalDocumentsPage() {
  const [page, setPage] = useState(0)
  const documents = usePortalDocuments(page)
  const upload = usePortalUpload()

  return (
    <>
      <PageHeader title="Τα έγγραφά μου" description="Τιμολόγια, εκκαθαριστικά και δικαιολογητικά που έχετε στείλει στο γραφείο." />
      <Card className="mb-6 p-5">
        <DocumentUploadZone onUpload={(file, onProgress) => upload.mutateAsync({ file, onProgress })} />
      </Card>
      <Card>
        <CardHeader title="Αρχεία" description={documents.data ? `${documents.data.totalElements} συνολικά` : undefined} />
        {documents.isLoading && <Spinner />}
        {documents.isError && <div className="p-4"><ErrorAlert>{getErrorMessage(documents.error)}</ErrorAlert></div>}
        {documents.data?.content.length === 0 && (
          <EmptyState icon={<FolderOpen className="size-10" />} title="Δεν έχετε ανεβάσει έγγραφα" />
        )}
        {documents.data && documents.data.content.length > 0 && (
          <>
            <DocumentTable documents={documents.data.content} onDownload={(d) => void portalService.download(d)} />
            <Pagination page={documents.data} onPageChange={setPage} />
          </>
        )}
      </Card>
    </>
  )
}
