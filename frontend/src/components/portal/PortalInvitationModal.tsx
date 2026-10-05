import { useState } from 'react'
import { AlertTriangle, Check, Copy, Mail } from 'lucide-react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { Textarea } from '@/components/ui/FormField'
import { copyText } from '@/lib/clipboard'
import { getErrorMessage } from '@/lib/errors'

export interface PortalCredentials {
  fullName: string
  email: string
  password: string
  /** true for a password reset of an existing login, false for a new one. */
  reset: boolean
}

/** Private-network or loopback host: reachable from the office only, not from a client's home. */
function isLocalHost(hostname: string): boolean {
  return hostname === 'localhost' || /^127\./.test(hostname) || /^10\./.test(hostname) ||
    /^192\.168\./.test(hostname) || /^172\.(1[6-9]|2\d|3[01])\./.test(hostname) || hostname.endsWith('.local')
}

function invitationText(c: PortalCredentials, clientName: string, loginUrl: string): string {
  const intro = c.reset
    ? `Ο κωδικός σας για το portal πελατών του γραφείου μας («${clientName}») άλλαξε.`
    : `Σας δημιουργήσαμε πρόσβαση στο portal πελατών του λογιστικού μας γραφείου («${clientName}»). ` +
      'Από εκεί βλέπετε τις φορολογικές σας υποχρεώσεις και προθεσμίες, μας στέλνετε δικαιολογητικά και επικοινωνείτε μαζί μας.'
  return [
    `Καλησπέρα ${c.fullName},`,
    '',
    intro,
    '',
    `Διεύθυνση: ${loginUrl}`,
    `Email: ${c.email}`,
    `Κωδικός: ${c.password}`,
    '',
    'Μετά τη σύνδεση, αλλάξτε τον κωδικό σας από «Τα στοιχεία μου».',
  ].join('\n')
}

/**
 * Ready-to-send login details for a client, shown right after the office creates a portal login
 * or resets its password (the only moment the password is known in clear).
 */
export function PortalInvitationModal({ credentials, clientName, onClose }: {
  credentials: PortalCredentials
  clientName: string
  onClose: () => void
}) {
  const loginUrl = `${window.location.origin}/login`
  const [text, setText] = useState(() => invitationText(credentials, clientName, loginUrl))
  const [copied, setCopied] = useState(false)
  const [copyError, setCopyError] = useState<string | null>(null)
  const subject = credentials.reset ? 'Νέος κωδικός για το portal πελατών' : 'Πρόσβαση στο portal πελατών'

  const copy = async () => {
    setCopyError(null)
    try {
      await copyText(text)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch (err) {
      setCopyError(getErrorMessage(err))
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      size="lg"
      title={credentials.reset ? 'Στείλτε τον νέο κωδικό στον πελάτη' : 'Στείλτε την πρόσκληση στον πελάτη'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Κλείσιμο</Button>
          <a href={`mailto:${credentials.email}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(text)}`}
            className="inline-flex items-center gap-2 rounded-lg bg-white px-3.5 py-2 text-sm font-medium text-slate-700 ring-1 ring-inset ring-slate-300 hover:bg-slate-50">
            <Mail className="size-4" aria-hidden /> Άνοιγμα στο email
          </a>
          <Button icon={copied ? <Check className="size-4" /> : <Copy className="size-4" />} onClick={() => void copy()}>
            {copied ? 'Αντιγράφηκε' : 'Αντιγραφή'}
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        <p className="text-sm text-slate-600">
          Ο κωδικός εμφανίζεται μόνο τώρα. Στείλτε τον στον πελάτη με email ή με όποιον τρόπο προτιμάτε.
        </p>
        {isLocalHost(window.location.hostname) && (
          <div className="flex gap-2 rounded-lg bg-amber-50 p-3 text-sm text-amber-800 ring-1 ring-amber-200">
            <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
            <span>
              Η διεύθυνση <span className="font-mono">{loginUrl}</span> λειτουργεί μόνο μέσα από το γραφείο. Για να μπαίνουν οι
              πελάτες από το σπίτι τους, η εφαρμογή πρέπει να δημοσιευθεί σε διεύθυνση internet με HTTPS.
            </span>
          </div>
        )}
        {copyError && <p className="text-sm text-red-600">{copyError}</p>}
        <Textarea rows={11} value={text} onChange={(e) => setText(e.target.value)} aria-label="Κείμενο πρόσκλησης" />
      </div>
    </Modal>
  )
}
