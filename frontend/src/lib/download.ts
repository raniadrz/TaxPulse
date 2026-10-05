/**
 * Hands a downloaded blob to the browser as a file. The link must be in the document for Firefox,
 * and the object URL is released only after the download has started (revoking it synchronously
 * can cancel the download in Safari/Firefox).
 */
export function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.style.display = 'none'
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 60_000)
}
