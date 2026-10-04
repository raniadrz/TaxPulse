import { Download, FileImage, FileSpreadsheet, FileText, RefreshCw, Trash2 } from 'lucide-react'
import { formatBytes, formatDateTime } from '@/lib/format'
import type { DocumentInfo } from '@/types/api'
import { DocumentStatusBadge } from './DocumentStatusBadge'

function fileIcon(contentType: string) {
  if (contentType.startsWith('image/')) return FileImage
  if (contentType.includes('spreadsheet') || contentType === 'text/csv') return FileSpreadsheet
  return FileText
}

interface DocumentTableProps {
  documents: DocumentInfo[]
  onDownload: (doc: DocumentInfo) => void
  onReindex?: (doc: DocumentInfo) => void
  onDelete?: (doc: DocumentInfo) => void
}

export function DocumentTable({ documents, onDownload, onReindex, onDelete }: DocumentTableProps) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
          <tr>
            <th scope="col" className="px-5 py-3">Αρχείο</th>
            <th scope="col" className="px-3 py-3">Ανέβηκε</th>
            <th scope="col" className="px-3 py-3">Ευρετηρίαση</th>
            <th scope="col" className="px-5 py-3"><span className="sr-only">Ενέργειες</span></th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 bg-white">
          {documents.map((d) => {
            const Icon = fileIcon(d.contentType)
            const canReindex = onReindex && (d.ingestionStatus === 'FAILED' || d.ingestionStatus === 'INDEXED') &&
              (d.contentType === 'application/pdf' || d.contentType.startsWith('text/'))
            return (
              <tr key={d.id} className="hover:bg-slate-50">
                <td className="px-5 py-3">
                  <div className="flex items-center gap-3">
                    <Icon className="size-5 shrink-0 text-slate-400" aria-hidden />
                    <div className="min-w-0">
                      <div className="truncate font-medium text-slate-900">{d.originalFilename}</div>
                      <div className="text-xs text-slate-500">{formatBytes(d.sizeBytes)}</div>
                    </div>
                  </div>
                </td>
                <td className="px-3 py-3 text-slate-600">
                  <div className="whitespace-nowrap">{formatDateTime(d.createdAt)}</div>
                  {d.uploadedBy && <div className="text-xs text-slate-500">{d.uploadedBy}</div>}
                </td>
                <td className="px-3 py-3">
                  <DocumentStatusBadge status={d.ingestionStatus} error={d.ingestionError} />
                  {d.ingestionStatus === 'FAILED' && d.ingestionError && (
                    <div className="mt-1 max-w-xs truncate text-xs text-red-600" title={d.ingestionError}>{d.ingestionError}</div>
                  )}
                </td>
                <td className="px-5 py-3">
                  <div className="flex justify-end gap-1">
                    <IconButton label="Λήψη" onClick={() => onDownload(d)} icon={Download} />
                    {canReindex && <IconButton label="Επανευρετηρίαση" onClick={() => onReindex(d)} icon={RefreshCw} />}
                    {onDelete && <IconButton label="Διαγραφή" onClick={() => onDelete(d)} icon={Trash2} danger />}
                  </div>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

function IconButton({ label, onClick, icon: Icon, danger }: { label: string; onClick: () => void; icon: typeof Download; danger?: boolean }) {
  return (
    <button
      type="button"
      onClick={onClick}
      title={label}
      aria-label={label}
      className={`rounded-md p-1.5 text-slate-400 hover:bg-slate-100 ${danger ? 'hover:text-red-600' : 'hover:text-slate-700'}`}
    >
      <Icon className="size-4" />
    </button>
  )
}
