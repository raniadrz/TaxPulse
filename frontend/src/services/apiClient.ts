import axios, { AxiosError, type AxiosInstance } from 'axios'
import { env } from '@/config/env'
import { tokenStorage } from '@/lib/tokenStorage'

/** Event fired when the backend rejects the token, so the auth context can log the user out. */
export const UNAUTHORIZED_EVENT = 'taxpulse:unauthorized'

/**
 * Single configured Axios instance used by every service module.
 * - attaches the JWT bearer token
 * - serialises arrays as repeated params (?status=A&status=B), which Spring binds to List<>
 * - broadcasts 401 responses
 */
export const apiClient: AxiosInstance = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 30_000,
  headers: { Accept: 'application/json' },
  paramsSerializer: { indexes: null },
})

apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const isLoginCall = error.config?.url?.includes('/auth/login')
    if (error.response?.status === 401 && !isLoginCall) {
      tokenStorage.clear()
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
    }
    return Promise.reject(error)
  },
)

/** Local LLM inference can take a while on CPU-only hosts. */
export const AI_TIMEOUT_MS = 180_000
