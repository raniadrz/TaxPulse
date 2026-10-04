import { NavLink } from 'react-router-dom'
import { Activity, ShieldCheck, X } from 'lucide-react'
import { cn } from '@/lib/cn'
import { navItems } from './navigation'

interface SidebarProps {
  open: boolean
  onClose: () => void
}

/** Primary navigation. Static on desktop, slide-over drawer on mobile. */
export function Sidebar({ open, onClose }: SidebarProps) {
  return (
    <>
      {open && <div className="fixed inset-0 z-30 bg-slate-900/40 lg:hidden" onClick={onClose} aria-hidden />}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-slate-900 text-slate-300 transition-transform lg:static lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex h-16 items-center justify-between px-5">
          <div className="flex items-center gap-2 text-white">
            <span className="flex size-8 items-center justify-center rounded-lg bg-brand-600">
              <Activity className="size-5" />
            </span>
            <span className="text-lg font-semibold tracking-tight">TaxPulse AI</span>
          </div>
          <button type="button" className="rounded p-1 hover:bg-slate-800 lg:hidden" onClick={onClose} aria-label="Κλείσιμο μενού">
            <X className="size-5" />
          </button>
        </div>

        <nav className="flex-1 space-y-1 px-3 py-4">
          {navItems.map(({ to, label, icon: Icon, disabled }) =>
            disabled ? (
              <span key={to} className="flex cursor-not-allowed items-center gap-3 rounded-lg px-3 py-2 text-sm text-slate-500">
                <Icon className="size-5" />
                {label}
                <span className="ml-auto rounded bg-slate-800 px-1.5 py-0.5 text-[10px] uppercase">Σύντομα</span>
              </span>
            ) : (
              <NavLink
                key={to}
                to={to}
                end={to === '/'}
                onClick={onClose}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                    isActive ? 'bg-slate-800 text-white' : 'hover:bg-slate-800/60 hover:text-white',
                  )
                }
              >
                <Icon className="size-5" />
                {label}
              </NavLink>
            ),
          )}
        </nav>

        <div className="m-3 flex items-start gap-2 rounded-lg bg-slate-800/60 p-3 text-xs text-slate-400">
          <ShieldCheck className="size-4 shrink-0 text-emerald-400" />
          <span>Τοπική επεξεργασία AI — τα δεδομένα δεν φεύγουν από το γραφείο (GDPR).</span>
        </div>
      </aside>
    </>
  )
}
