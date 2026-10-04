// Types mirroring the Spring Boot DTOs (gr.taxpulse.*.dto). Keep in sync with the backend.

export type UUID = string
/** ISO-8601 date (yyyy-MM-dd). */
export type IsoDate = string
/** ISO-8601 instant. */
export type IsoDateTime = string

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

/** RFC 9457 problem details returned by the backend on errors. */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  errors?: Record<string, string>
}

// ---- Auth / users ----
export type Role = 'ADMIN' | 'ACCOUNTANT' | 'ASSISTANT'

export interface User {
  id: UUID
  email: string
  fullName: string
  role: Role
  active: boolean
  lastLoginAt?: IsoDateTime
  createdAt: IsoDateTime
}

export interface AuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresAt: IsoDateTime
  user: User
}

// ---- Clients ----
export type ClientType = 'INDIVIDUAL' | 'LEGAL_ENTITY'
export type BookCategory = 'NONE' | 'A' | 'B' | 'C'

export interface Address {
  street?: string
  city?: string
  postalCode?: string
}

export interface ActivityCode {
  code: string
  description?: string
  primary: boolean
}

export interface Representative {
  id?: UUID
  fullName: string
  afm?: string
  role?: string
  email?: string
  phone?: string
  primary: boolean
}

export interface ClientSummary {
  id: UUID
  clientType: ClientType
  afm: string
  name: string
  doy: string
  bookCategory: BookCategory
  primaryKad?: string
  email?: string
  phone?: string
  assignedAccountantName?: string
  active: boolean
  openObligations: number
  overdueObligations: number
  nextDueDate?: IsoDate
}

export interface Client {
  id: UUID
  clientType: ClientType
  afm: string
  doy: string
  name: string
  tradeName?: string
  legalForm?: string
  bookCategory: BookCategory
  gemiNumber?: string
  email?: string
  phone?: string
  mobile?: string
  address?: Address
  assignedAccountant?: { id: UUID; fullName: string }
  notes?: string
  active: boolean
  activityCodes: ActivityCode[]
  representatives: Representative[]
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
  version: number
}

export interface ClientRequest {
  clientType: ClientType
  afm: string
  doy: string
  name: string
  tradeName?: string
  legalForm?: string
  bookCategory: BookCategory
  gemiNumber?: string
  email?: string
  phone?: string
  mobile?: string
  address?: Address
  assignedAccountantId?: UUID
  notes?: string
  active?: boolean
  activityCodes?: ActivityCode[]
  representatives?: Representative[]
}

export interface ClientSearchParams {
  q?: string
  type?: ClientType
  bookCategory?: BookCategory
  active?: boolean
  page?: number
  size?: number
  sort?: string
}

// ---- Obligations ----
export type ObligationType =
  | 'VAT'
  | 'INCOME_TAX'
  | 'APD'
  | 'MYDATA'
  | 'GEMI'
  | 'ENFIA'
  | 'WITHHOLDING_TAX'
  | 'PAYROLL'
  | 'INTRASTAT'
  | 'OTHER'

export type ObligationStatus = 'PENDING_DOCS' | 'IN_PROGRESS' | 'SUBMITTED' | 'OVERDUE'

export interface Obligation {
  id: UUID
  client: { id: UUID; name: string; afm: string }
  obligationType: ObligationType
  obligationTypeLabel: string
  title: string
  description?: string
  periodStart?: IsoDate
  periodEnd?: IsoDate
  dueDate: IsoDate
  daysUntilDue: number
  status: ObligationStatus
  amount?: number
  assignedTo?: { id: UUID; fullName: string }
  submittedAt?: IsoDateTime
  submissionRef?: string
  notes?: string
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
  version: number
}

export interface ObligationRequest {
  clientId: UUID
  obligationType: ObligationType
  title: string
  description?: string
  periodStart?: IsoDate
  periodEnd?: IsoDate
  dueDate: IsoDate
  amount?: number
  assignedToId?: UUID
  notes?: string
}

export interface ObligationSearchParams {
  clientId?: UUID
  status?: ObligationStatus[]
  type?: ObligationType
  dueFrom?: IsoDate
  dueTo?: IsoDate
  mine?: boolean
  page?: number
  size?: number
  sort?: string
}

// ---- Dashboard ----
export interface DashboardStats {
  activeClients: number
  openObligations: number
  overdueObligations: number
  dueWithin7Days: number
  submittedThisMonth: number
  obligationsByStatus: Record<ObligationStatus, number>
  upcomingDeadlines: Obligation[]
}

// ---- Notifications ----
export type NotificationType = 'DEADLINE_UPCOMING' | 'DEADLINE_OVERDUE' | 'DOCUMENT_PROCESSED' | 'SYSTEM'

export interface AppNotification {
  id: UUID
  type: NotificationType
  title: string
  message: string
  obligationId?: UUID
  clientId?: UUID
  read: boolean
  createdAt: IsoDateTime
}

// ---- AI ----
export type ChatRole = 'USER' | 'ASSISTANT'

export interface ChatMessage {
  role: ChatRole
  content: string
}

export interface ChatRequest {
  messages: ChatMessage[]
  clientId?: UUID
  useDocuments?: boolean
}

export interface SourceRef {
  documentId: UUID
  filename: string
  chunkIndex: number
  score: number
}

export interface ChatResponse {
  reply: string
  model: string
  sources: SourceRef[]
}

export interface ExtractedRecord {
  afm?: string
  afmValid: boolean
  partyName?: string
  amount?: number
  currency?: string
  date?: IsoDate
  documentType?: string
  obligationType?: ObligationType
  description?: string
  matchedClient?: { id: UUID; name: string }
}

export interface ExtractionResponse {
  records: ExtractedRecord[]
  warnings: string[]
  model: string
}

export type EmailTone = 'FORMAL' | 'FRIENDLY' | 'URGENT'

export interface ReminderEmailResponse {
  subject: string
  body: string
  recipientName?: string
  recipientEmail?: string
}

export interface OllamaHealth {
  reachable: boolean
  baseUrl: string
  chatModel: string
  chatModelAvailable: boolean
  embeddingModel: string
  embeddingModelAvailable: boolean
  installedModels: string[]
  error?: string
}
