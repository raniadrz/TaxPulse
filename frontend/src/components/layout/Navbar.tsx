import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { LogOut, Menu, Search } from 'lucide-react'
import { useAuth } from '@/hooks/useAuth'
import { roleLabel } from '@/lib/labels'
import { NotificationBell } from './NotificationBell'

/** Top bar: mobile menu toggle, global ΑΦΜ / name search, notifications and user menu. */
export function Navbar({ onMenuClick }: { onMenuClick: () => void }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [query, setQuery] = useState('')

  const onSearch = (e: FormEvent) => {
    e.preventDefault()
    const q = query.trim()
    if (q) navigate(`/clients?q=${encodeURIComponent(q)}`)
  }

  const initials = user?.fullName
    .split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-slate-200 bg-white/90 px-4 backdrop-blur sm:px-6">
      <button type="button" className="rounded p-2 text-slate-500 hover:bg-slate-100 lg:hidden" onClick={onMenuClick} aria-label="Άνοιγμα μενού">
        <Menu className="size-5" />
      </button>

      <form onSubmit={onSearch} className="relative max-w-md flex-1">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
        <input
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Αναζήτηση πελάτη με ΑΦΜ ή επωνυμία…"
          className="w-full rounded-lg border-0 bg-slate-100 py-2 pl-9 pr-3 text-sm placeholder:text-slate-400 focus:bg-white focus:ring-2 focus:ring-brand-600"
        />
      </form>

      <div className="ml-auto flex items-center gap-2">
        <NotificationBell />
        <div className="hidden items-center gap-3 border-l border-slate-200 pl-3 sm:flex">
          <span className="flex size-9 items-center justify-center rounded-full bg-brand-100 text-sm font-semibold text-brand-700">
            {initials}
          </span>
          <div className="leading-tight">
            <div className="text-sm font-medium text-slate-900">{user?.fullName}</div>
            <div className="text-xs text-slate-500">{user && roleLabel[user.role]}</div>
          </div>
        </div>
        <button type="button" onClick={logout} className="rounded-full p-2 text-slate-500 hover:bg-slate-100" aria-label="Αποσύνδεση" title="Αποσύνδεση">
          <LogOut className="size-5" />
        </button>
      </div>
    </header>
  )
}
