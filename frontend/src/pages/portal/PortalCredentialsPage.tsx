import { PageHeader } from '@/components/layout/PageHeader'
import { CredentialsPanel } from '@/components/credentials/CredentialsPanel'

export default function PortalCredentialsPage() {
  return (
    <>
      <PageHeader title="Κωδικοί πρόσβασης"
        description="Οι κωδικοί TAXISnet και e-ΕΦΚΑ που χρησιμοποιεί το γραφείο για τις δηλώσεις σας. Αν τους αλλάξετε στην ΑΑΔΕ, ενημερώστε τους κι εδώ." />
      <div className="max-w-3xl">
        <CredentialsPanel scope={{ kind: 'portal' }} />
      </div>
    </>
  )
}
