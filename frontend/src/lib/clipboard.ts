/**
 * Copies text to the clipboard. `navigator.clipboard` only exists in secure contexts (https or
 * localhost), so when the office opens the app as http://<server-ip>:8081 it falls back to the
 * legacy copy command on a temporary textarea.
 */
export async function copyText(text: string): Promise<void> {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text)
    return
  }
  const area = document.createElement('textarea')
  area.value = text
  area.setAttribute('readonly', '')
  area.style.position = 'fixed'
  area.style.opacity = '0'
  // Inside an open <dialog> only its own subtree is focusable, so attach there when present.
  const host = document.querySelector('dialog[open]') ?? document.body
  host.appendChild(area)
  area.select()
  const ok = document.execCommand('copy')
  area.remove()
  if (!ok) throw new Error('Η αντιγραφή απέτυχε. Επιλέξτε το κείμενο και αντιγράψτε το χειροκίνητα.')
}
