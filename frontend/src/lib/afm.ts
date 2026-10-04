/** Client-side ΑΦΜ check-digit validation (mirrors the backend AfmValidator) for instant feedback. */
export function isValidAfm(afm: string): boolean {
  if (!/^\d{9}$/.test(afm) || afm === '000000000') return false
  let sum = 0
  for (let i = 0; i < 8; i++) sum += Number(afm[i]) * 2 ** (8 - i)
  return (sum % 11) % 10 === Number(afm[8])
}
