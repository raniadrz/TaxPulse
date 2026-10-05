import { PageHeader } from '@/components/layout/PageHeader'
import { AskDocumentsPanel } from '@/components/documents/AskDocumentsPanel'
import { Spinner } from '@/components/ui/Spinner'
import { usePortalDocuments } from '@/hooks/usePortal'
import { portalService } from '@/services/portalService'

export default function PortalAssistantPage() {
  const documents = usePortalDocuments(0)
  const indexedCount = documents.data?.content.filter((d) => d.ingestionStatus === 'INDEXED').length ?? 0

  return (
    <>
      <PageHeader title="Ρωτήστε το AI"
        description="Ερωτήσεις πάνω στα δικά σας έγγραφα, π.χ. ποσά τιμολογίων ή ΦΠΑ. Η επεξεργασία γίνεται τοπικά στο γραφείο." />
      <div className="max-w-3xl">
        {documents.isLoading ? <Spinner /> : <AskDocumentsPanel onAsk={portalService.ask} indexedCount={indexedCount} />}
      </div>
    </>
  )
}
