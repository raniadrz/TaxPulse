import { isAxiosError } from 'axios'
import type { ProblemDetail } from '@/types/api'

/** Extracts a human readable message from an API error (RFC 9457 problem details). */
export function getErrorMessage(error: unknown, fallback = 'Παρουσιάστηκε σφάλμα. Δοκιμάστε ξανά.'): string {
  if (isAxiosError<ProblemDetail>(error)) {
    if (!error.response) return 'Δεν υπάρχει σύνδεση με τον διακομιστή.'
    const problem = error.response.data
    if (problem?.errors) {
      const first = Object.values(problem.errors)[0]
      if (first) return first
    }
    return problem?.detail ?? problem?.title ?? fallback
  }
  return error instanceof Error ? error.message : fallback
}

/** Field-level validation errors keyed by field name, if any. */
export function getFieldErrors(error: unknown): Record<string, string> {
  if (isAxiosError<ProblemDetail>(error)) return error.response?.data?.errors ?? {}
  return {}
}
