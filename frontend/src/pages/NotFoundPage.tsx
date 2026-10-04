import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center py-24 text-center">
      <p className="text-sm font-semibold text-brand-600">404</p>
      <h1 className="mt-2 text-2xl font-semibold">Η σελίδα δεν βρέθηκε</h1>
      <Link to="/" className="mt-6 text-sm font-medium text-brand-600 hover:underline">
        Επιστροφή στον πίνακα ελέγχου
      </Link>
    </div>
  )
}
