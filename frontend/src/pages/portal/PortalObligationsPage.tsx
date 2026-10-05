import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { CheckCircle2 } from 'lucide-react'
import { PageHeader } from '@/components/layout/PageHeader'
import { PortalObligationTable } from '@/components/portal/PortalObligationTable'
import { ObligationMessagesModal } from '@/components/messages/ObligationMessagesModal'
import { Card } from '@/components/ui/Card'
import { Spinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/Alert'
import { usePortalObligations } from '@/hooks/usePortal'
import { getErrorMessage } from '@/lib/errors'
import type { PortalObligation } from '@/types/api'
import { cn } from '@/lib/cn'
import { DONE_STATUSES, OPEN_STATUSES } from './portalStatuses'

const TABS = [['open', 'Σε εκκρεμότητα'], ['done', 'Υποβληθείσες']] as const

export default function PortalObligationsPage() {
  const [tab, setTab] = useState<'open' | 'done'>('open')
  const obligations = usePortalObligations(tab === 'open' ? OPEN_STATUSES : DONE_STATUSES, tab === 'open' ? 'dueDate,asc' : 'dueDate,desc')
  // ?messages=<obligationId> (from a notification) opens that obligation's conversation.
  const [params, setParams] = useSearchParams()
  const [selected, setSelected] = useState<PortalObligation | null>(null)
  const threadId = selected?.id ?? params.get('messages')
  const threadTitle = selected?.title ?? obligations.data?.content.find((o) => o.id === threadId)?.title
  const closeThread = () => {
    setSelected(null)
    if (params.has('messages')) setParams({}, { replace: true })
  }

  return (
    <>
      <PageHeader title="Οι υποχρεώσεις μου"
        description="Πατήστε «Δικαιολογητικά» για να στείλετε έγγραφα στο γραφείο ή «Μηνύματα» για να του γράψετε." />
      <div className="mb-4 inline-flex rounded-lg bg-slate-200/60 p-0.5 text-sm" role="tablist">
        {TABS.map(([key, label]) => (
          <button key={key} type="button" role="tab" aria-selected={tab === key} onClick={() => setTab(key)}
            className={cn('rounded-md px-3 py-1.5 font-medium', tab === key ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900')}>
            {label}
          </button>
        ))}
      </div>
      <Card>
        {obligations.isLoading && <Spinner />}
        {obligations.isError && <div className="p-4"><ErrorAlert>{getErrorMessage(obligations.error)}</ErrorAlert></div>}
        {obligations.data?.content.length === 0 && (
          <EmptyState icon={<CheckCircle2 className="size-10" />}
            title={tab === 'open' ? 'Δεν υπάρχουν εκκρεμότητες' : 'Καμία υποβολή ακόμη'}
            description={tab === 'open' ? 'Θα ειδοποιηθείτε όταν χρειαστεί κάτι από εσάς.' : undefined} />
        )}
        {obligations.data && obligations.data.content.length > 0 && <PortalObligationTable obligations={obligations.data.content} onMessages={setSelected} />}
      </Card>
      {threadId && <ObligationMessagesModal key={threadId} scope="portal" obligationId={threadId} title={threadTitle} onClose={closeThread} />}
    </>
  )
}
