import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import App from './App'
import './index.css'

/**
 * After a redeploy, a tab still running the previous build asks for code-split chunks whose
 * hashed names no longer exist (404). Reload once to pick up the new build; the timestamp guard
 * prevents a reload loop if the chunk is genuinely missing.
 */
const RELOAD_KEY = 'taxpulse.chunkReloadAt'
window.addEventListener('vite:preloadError', (event) => {
  let last = 0
  try {
    last = Number(sessionStorage.getItem(RELOAD_KEY) ?? 0)
  } catch {
    // storage unavailable: fall through and reload once
  }
  if (Date.now() - last < 10_000) return
  try {
    sessionStorage.setItem(RELOAD_KEY, String(Date.now()))
  } catch {
    // ignore
  }
  event.preventDefault()
  window.location.reload()
})

/** Shared React Query cache: sensible defaults for an internal business app. */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
