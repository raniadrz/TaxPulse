/**
 * Access-token persistence. Isolated in one module so the strategy can be hardened later
 * (e.g. in-memory token + httpOnly refresh cookie) without touching the rest of the app.
 */
const TOKEN_KEY = 'taxpulse.accessToken'

export const tokenStorage = {
  get(): string | null {
    return localStorage.getItem(TOKEN_KEY)
  },
  set(token: string): void {
    localStorage.setItem(TOKEN_KEY, token)
  },
  clear(): void {
    localStorage.removeItem(TOKEN_KEY)
  },
}
