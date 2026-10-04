import { useState } from 'react'
import { Input, Select } from '@/components/ui/FormField'
import { useClients } from '@/hooks/useClients'
import { useDebounce } from '@/hooks/useDebounce'
import type { UUID } from '@/types/api'

interface ClientPickerProps {
  value: UUID | ''
  onChange: (clientId: UUID | '') => void
  required?: boolean
  placeholder?: string
}

/** Searchable client selector: a debounced ΑΦΜ / name filter feeding a native select. */
export function ClientPicker({ value, onChange, required, placeholder = '— Επιλέξτε πελάτη —' }: ClientPickerProps) {
  const [query, setQuery] = useState('')
  const debounced = useDebounce(query.trim())
  const { data } = useClients({ q: debounced || undefined, active: true, size: 20, sort: 'name,asc' })

  return (
    <div className="space-y-2">
      <Input placeholder="Αναζήτηση με ΑΦΜ ή επωνυμία…" value={query} onChange={(e) => setQuery(e.target.value)} aria-label="Αναζήτηση πελάτη" />
      <Select required={required} value={value} onChange={(e) => onChange(e.target.value)} aria-label="Πελάτης">
        <option value="">{placeholder}</option>
        {data?.content.map((c) => (
          <option key={c.id} value={c.id}>
            {c.name} ({c.afm})
          </option>
        ))}
      </Select>
    </div>
  )
}
