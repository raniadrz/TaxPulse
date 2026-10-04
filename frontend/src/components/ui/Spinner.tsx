import { Loader2 } from 'lucide-react'
import { cn } from '@/lib/cn'

export function Spinner({ className, label = 'Φόρτωση…' }: { className?: string; label?: string }) {
  return (
    <div role="status" className={cn('flex items-center justify-center gap-2 py-10 text-sm text-slate-500', className)}>
      <Loader2 className="size-5 animate-spin text-brand-600" aria-hidden />
      <span>{label}</span>
    </div>
  )
}
