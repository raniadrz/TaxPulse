import type { BookCategory, ClientType, ObligationStatus, ObligationType, Role } from '@/types/api'

/** Greek UI labels for backend enums. */
export const obligationStatusLabel: Record<ObligationStatus, string> = {
  PENDING_DOCS: 'Αναμονή δικαιολογητικών',
  IN_PROGRESS: 'Σε εξέλιξη',
  SUBMITTED: 'Υποβλήθηκε',
  OVERDUE: 'Εκπρόθεσμη',
}

export const obligationTypeLabel: Record<ObligationType, string> = {
  VAT: 'ΦΠΑ',
  INCOME_TAX: 'Φόρος Εισοδήματος',
  APD: 'ΑΠΔ',
  MYDATA: 'myDATA',
  GEMI: 'ΓΕΜΗ',
  ENFIA: 'ΕΝΦΙΑ',
  WITHHOLDING_TAX: 'Παρακρατούμενοι Φόροι',
  PAYROLL: 'Μισθοδοσία',
  INTRASTAT: 'Intrastat',
  OTHER: 'Λοιπά',
}

export const clientTypeLabel: Record<ClientType, string> = {
  INDIVIDUAL: 'Φυσικό Πρόσωπο',
  LEGAL_ENTITY: 'Νομικό Πρόσωπο',
}

export const bookCategoryLabel: Record<BookCategory, string> = {
  NONE: 'Χωρίς βιβλία',
  A: "Α' κατηγορίας",
  B: "Β' κατηγορίας",
  C: "Γ' κατηγορίας",
}

export const roleLabel: Record<Role, string> = {
  ADMIN: 'Διαχειριστής',
  ACCOUNTANT: 'Λογιστής',
  ASSISTANT: 'Βοηθός',
}
