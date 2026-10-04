/** Runtime configuration resolved from Vite env variables (build time). */
export const env = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
} as const
