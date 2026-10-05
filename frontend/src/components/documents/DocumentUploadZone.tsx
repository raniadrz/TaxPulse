import { useRef, useState, type DragEvent } from 'react'
import { UploadCloud } from 'lucide-react'
import { getErrorMessage } from '@/lib/errors'
import { cn } from '@/lib/cn'

/** Must match the backend whitelist (DocumentService.ALLOWED_TYPES). */
const ACCEPT = '.pdf,.txt,.csv,.md,.png,.jpg,.jpeg,.xlsx,.docx'
const MAX_BYTES = 25 * 1024 * 1024

interface UploadState {
  name: string
  progress: number
  error?: string
}

/**
 * Drag & drop / click-to-browse uploader. Files are sent one by one so progress is per file.
 * The caller decides where a file goes (staff upload for a client, or the client portal).
 */
export function DocumentUploadZone({ onUpload }: { onUpload: (file: File, onProgress: (percent: number) => void) => Promise<unknown> }) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = useState(false)
  const [uploads, setUploads] = useState<UploadState[]>([])

  const update = (name: string, patch: Partial<UploadState>) =>
    setUploads((list) => list.map((u) => (u.name === name ? { ...u, ...patch } : u)))

  const handleFiles = async (files: FileList | null) => {
    if (!files?.length) return
    const batch = Array.from(files)
    setUploads(batch.map((f) => ({ name: f.name, progress: 0 })))
    for (const file of batch) {
      if (file.size > MAX_BYTES) {
        update(file.name, { error: 'Μέγιστο μέγεθος 25 MB' })
        continue
      }
      try {
        await onUpload(file, (progress) => update(file.name, { progress }))
        update(file.name, { progress: 100 })
      } catch (err) {
        update(file.name, { error: getErrorMessage(err) })
      }
    }
    // Keep only failures on screen; successful files now appear in the table.
    setUploads((list) => list.filter((u) => u.error))
  }

  const onDrop = (e: DragEvent) => {
    e.preventDefault()
    setDragging(false)
    void handleFiles(e.dataTransfer.files)
  }

  return (
    <div>
      <button
        type="button"
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        className={cn(
          'flex w-full flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-8 text-center transition-colors',
          dragging ? 'border-brand-500 bg-brand-50' : 'border-slate-300 hover:border-brand-400 hover:bg-slate-50',
        )}
      >
        <UploadCloud className="mb-2 size-8 text-slate-400" aria-hidden />
        <span className="text-sm font-medium text-slate-700">Σύρετε αρχεία εδώ ή κάντε κλικ για επιλογή</span>
        <span className="mt-1 text-xs text-slate-500">PDF, TXT, CSV, εικόνες, Word/Excel · έως 25 MB. Τα PDF/TXT ευρετηριάζονται για τον AI Copilot.</span>
      </button>
      <input ref={inputRef} type="file" multiple accept={ACCEPT} className="hidden" onChange={(e) => {
        void handleFiles(e.target.files)
        e.target.value = ''
      }} />

      {uploads.length > 0 && (
        <ul className="mt-3 space-y-2">
          {uploads.map((u) => (
            <li key={u.name} className="text-sm">
              <div className="flex justify-between gap-2">
                <span className="truncate text-slate-700">{u.name}</span>
                <span className={u.error ? 'text-red-600' : 'text-slate-500'}>{u.error ?? `${u.progress}%`}</span>
              </div>
              {!u.error && (
                <div className="mt-1 h-1.5 rounded-full bg-slate-100">
                  <div className="h-full rounded-full bg-brand-600 transition-all" style={{ width: `${u.progress}%` }} />
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
