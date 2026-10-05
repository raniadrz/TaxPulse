import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertOctagon, Bell, CalendarClock, CheckCheck, CircleCheckBig, FileCheck2, FileInput, KeyRound, MessageSquare } from 'lucide-react'
import { useMarkNotificationsRead, useNotifications, useUnreadNotificationCount } from '@/hooks/useNotifications'
import { useAuth } from '@/hooks/useAuth'
import { formatDateTime } from '@/lib/format'
import { notificationLink } from '@/lib/notificationLink'
import { cn } from '@/lib/cn'
import type { AppNotification, NotificationType } from '@/types/api'

const icons: Record<NotificationType, typeof Bell> = {
  DEADLINE_UPCOMING: CalendarClock,
  DEADLINE_OVERDUE: AlertOctagon,
  DOCUMENT_PROCESSED: FileCheck2,
  SYSTEM: Bell,
  MESSAGE: MessageSquare,
  STATUS_CHANGED: CircleCheckBig,
  DOCUMENT_RECEIVED: FileInput,
  CREDENTIALS_UPDATED: KeyRound,
}

/** Navbar bell with unread badge and a dropdown of the latest notifications; each one opens what it is about. */
export function NotificationBell() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  const { data: unread = 0 } = useUnreadNotificationCount()
  const { data, isLoading } = useNotifications(open)
  const { markOne, markAll } = useMarkNotificationsRead()

  const onSelect = (n: AppNotification) => {
    if (!n.read) markOne.mutate(n.id)
    const to = user && notificationLink(n, user.role)
    if (to) {
      setOpen(false)
      navigate(to)
    }
  }

  useEffect(() => {
    if (!open) return
    const onClick = (e: MouseEvent) => ref.current && !ref.current.contains(e.target as Node) && setOpen(false)
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [open])

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="relative rounded-full p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-700"
        aria-label={`Ειδοποιήσεις (${unread} μη αναγνωσμένες)`}
      >
        <Bell className="size-5" />
        {unread > 0 && (
          <span className="absolute right-1 top-1 flex min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-semibold text-white">
            {unread > 99 ? '99+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 z-50 mt-2 w-96 max-w-[calc(100vw-2rem)] rounded-xl border border-slate-200 bg-white shadow-lg">
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
            <span className="text-sm font-semibold">Ειδοποιήσεις</span>
            <button
              type="button"
              className="flex items-center gap-1 text-xs text-brand-600 hover:underline disabled:opacity-50"
              onClick={() => markAll.mutate()}
              disabled={unread === 0}
            >
              <CheckCheck className="size-3.5" /> Όλες αναγνωσμένες
            </button>
          </div>
          <ul className="max-h-96 divide-y divide-slate-100 overflow-y-auto">
            {isLoading && <li className="px-4 py-6 text-center text-sm text-slate-500">Φόρτωση…</li>}
            {data?.content.length === 0 && <li className="px-4 py-6 text-center text-sm text-slate-500">Καμία ειδοποίηση</li>}
            {data?.content.map((n) => {
              const Icon = icons[n.type]
              return (
                <li key={n.id}>
                  <button
                    type="button"
                    onClick={() => onSelect(n)}
                    className={cn('flex w-full gap-3 px-4 py-3 text-left hover:bg-slate-50', !n.read && 'bg-brand-50/50')}
                  >
                    <Icon className={cn('mt-0.5 size-4 shrink-0', n.type === 'DEADLINE_OVERDUE' ? 'text-red-500' : 'text-brand-600')} />
                    <span className="min-w-0">
                      <span className={cn('block text-sm', !n.read && 'font-semibold')}>{n.title}</span>
                      <span className="mt-0.5 block text-xs text-slate-500">{n.message}</span>
                      <span className="mt-1 block text-[11px] text-slate-400">{formatDateTime(n.createdAt)}</span>
                    </span>
                  </button>
                </li>
              )
            })}
          </ul>
        </div>
      )}
    </div>
  )
}
