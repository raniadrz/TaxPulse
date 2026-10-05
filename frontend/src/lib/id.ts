let counter = 0

/**
 * Local unique id for UI list keys. `crypto.randomUUID` only exists in secure contexts (https or
 * localhost), so it is undefined when the office opens the app as http://<server-ip>:8081.
 */
export function newId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') return crypto.randomUUID()
  counter += 1
  return `${Date.now().toString(36)}-${counter}-${Math.random().toString(36).slice(2, 10)}`
}
