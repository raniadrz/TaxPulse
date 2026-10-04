import { ChevronLeft, ChevronRight } from 'lucide-react'
import type { PageResponse } from '@/types/api'

export function Pagination<T>({ page, onPageChange }: { page: PageResponse<T>; onPageChange: (page: number) => void }) {
  if (page.totalPages <= 1) return null
  const from = page.page * page.size + 1
  const to = from + page.content.length - 1
  return (
    <div className="flex items-center justify-between border-t border-slate-100 px-5 py-3 text-sm text-slate-600">
      <span>
        {from}–{to} από {page.totalElements}
      </span>
      <div className="flex gap-1">
        <button
          type="button"
          className="rounded-md p-1.5 hover:bg-slate-100 disabled:opacity-40"
          disabled={page.page === 0}
          onClick={() => onPageChange(page.page - 1)}
          aria-label="Προηγούμενη σελίδα"
        >
          <ChevronLeft className="size-4" />
        </button>
        <button
          type="button"
          className="rounded-md p-1.5 hover:bg-slate-100 disabled:opacity-40"
          disabled={page.last}
          onClick={() => onPageChange(page.page + 1)}
          aria-label="Επόμενη σελίδα"
        >
          <ChevronRight className="size-4" />
        </button>
      </div>
    </div>
  )
}
