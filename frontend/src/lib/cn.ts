import clsx, { type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * Conditional className helper. `twMerge` resolves conflicting utilities so a caller's
 * override (e.g. `w-44` over a component's default `w-full`) reliably wins.
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs))
}
