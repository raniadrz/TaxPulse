# TaxPulse AI — Smart Accounting & Client Operations Platform

B2B SaaS πλατφόρμα για ελληνικά λογιστικά γραφεία: **CRM πελατών**, **φορολογικό ημερολόγιο με αυτόματες
υπενθυμίσεις**, **portal πελατών** και **τοπικός AI Copilot (Ollama)**. Τα δεδομένα δεν φεύγουν από το γραφείο, κάτι που
διευκολύνει τη συμμόρφωση με τον GDPR.

| Layer | Τεχνολογίες |
|---|---|
| Frontend | React 19, TypeScript, Vite, Tailwind CSS v4, Lucide, React Query, Axios, React Router |
| Backend | Spring Boot 3.5 (Java 21, virtual threads), Spring Security (JWT + RBAC), Spring Data JPA |
| Database | PostgreSQL 16 + pgvector, Flyway migrations |
| AI | Τοπικό Ollama (`llama3.2` / `qwen2.5-coder`, embeddings `nomic-embed-text`) |
| Infra | Docker, docker compose, nginx, GitHub Actions |

---

## 🚀 Γρήγορη εκκίνηση (Docker)

```bash
cp .env.example .env            # ορίστε POSTGRES_PASSWORD, JWT_SECRET (openssl rand -base64 48), admin credentials
docker compose up -d --build    # postgres + ollama (+ αυτόματο pull μοντέλων) + backend + frontend
```

| Υπηρεσία | URL |
|---|---|
| Εφαρμογή (nginx → SPA + `/api`) | http://localhost:8081 |
| REST API / Swagger UI | http://localhost:8080/swagger-ui.html |
| Ollama | http://localhost:11434 |

Συνδεθείτε με τα `TAXPULSE_ADMIN_EMAIL` / `TAXPULSE_ADMIN_PASSWORD` του `.env`. Ο διαχειριστής
δημιουργείται μόνο όταν ο πίνακας `users` είναι άδειος.

Το πρώτο `up` κατεβάζει τα μοντέλα (~2 GB) μέσω της υπηρεσίας `ollama-init`. Η πρόοδος φαίνεται με
`docker compose logs -f ollama-init`. Για GPU, αφαιρέστε τα σχόλια από το block `deploy` της υπηρεσίας `ollama`.

### Τοπική ανάπτυξη (χωρίς Docker για τον κώδικα)

```bash
docker compose up -d postgres ollama ollama-init         # μόνο οι εξαρτήσεις

cd backend
TAXPULSE_ADMIN_EMAIL=admin@taxpulse.local TAXPULSE_ADMIN_PASSWORD='ChangeMe!2026' \
DB_PASSWORD=<POSTGRES_PASSWORD> ./mvnw spring-boot:run   # :8080

cd frontend
npm install && npm run dev                               # :5173 (το Vite προωθεί το /api στο :8080)
```

### Tests

```bash
cd backend  && ./mvnw verify          # unit + integration (Testcontainers / pgvector, απαιτεί Docker)
cd frontend && npm run lint && npm run build
```

---

## 🏗️ Αρχιτεκτονική

```
 Browser ──► nginx (frontend) ──/api──► Spring Boot ──► PostgreSQL + pgvector
                                             │
                                             └──► Ollama (/api/chat, /api/generate, /api/embed)
```

Όλες οι υπηρεσίες τρέχουν στο κοινό δίκτυο `taxpulse`. Το backend δεν εκτίθεται στο κοινό. Οι θύρες
της βάσης, του Ollama και του API δένονται μόνο στο `127.0.0.1`.

### Backend: package-by-feature με layered sub-packages

```
backend/src/main/java/gr/taxpulse
├── config/          # typed properties, JPA auditing, async executor, OpenAPI, Clock, bootstrap admin
├── common/          # BaseEntity (UUID, auditing, @Version), ProblemDetail handler, PageResponse, ΑΦΜ validator
├── security/        # JWT service + filter, SecurityConfig (stateless, CORS), UserPrincipal, PasswordEncoder
├── auth/            # POST /auth/login, GET /auth/me
├── user/            # controller · service · repository · entity · dto · mapper
├── client/          # CRM: Client aggregate (ΚΑΔ, εκπρόσωποι, διεύθυνση), search specifications
├── obligation/      # φορολογικό ημερολόγιο, state machine, reminder job
├── notification/    # in-app ειδοποιήσεις, idempotent inserts, προαιρετικό email channel
├── document/        # uploads, local storage, text extraction (PDFBox)
├── dashboard/       # συγκεντρωτικά KPI
└── ai/
    ├── ollama/      # OllamaIntegrationService (RestClient) + ports ChatModelClient / EmbeddingModelClient
    ├── prompt/      # system prompts, JSON Schema builder για structured outputs
    ├── rag/         # TextChunker, PgVectorStore, DocumentIngestionService, RagService
    ├── service/     # use cases: chat, data extraction, reminder emails
    └── controller/
├── message/         # συζήτηση λογιστή και πελάτη ανά υποχρέωση
└── portal/          # portal πελατών: λογαριασμοί CLIENT, /api/v1/portal, ειδοποιήσεις ανάμεσα σε γραφείο και πελάτη
```

Αρχές σχεδίασης:
- **Dependency inversion**: οι AI use cases εξαρτώνται από τα interfaces `ChatModelClient`/`EmbeddingModelClient`,
  όχι από το Ollama. Το CRM παίρνει μετρητές υποχρεώσεων μέσω του `ClientObligationStatsPort`, το οποίο υλοποιεί
  το module υποχρεώσεων.
- **Rich domain model**: οι μεταβάσεις κατάστασης γίνονται μόνο μέσω `TaxObligation.transitionTo()`, ώστε να μην
  μπορεί να παρακαμφθεί η state machine.
- **DTOs + explicit mappers**: τα entities δεν φτάνουν ποτέ στο API. Τα λάθη επιστρέφονται ως RFC 9457 `ProblemDetail`.
- **Optimistic locking** (`@Version`): ταυτόχρονες επεξεργασίες απαντούν 409 αντί να γράφει η μία πάνω στην άλλη.

### Frontend

```
frontend/src
├── components/
│   ├── ui/            # Button, Card, Badge, Modal (<dialog>), FormField, Pagination…
│   ├── layout/        # AppLayout, Sidebar, Navbar, NotificationBell
│   ├── dashboard/     # StatCard, StatusBreakdown, UpcomingDeadlines
│   ├── clients/       # ClientTable, ClientFormModal, ClientComplianceBadge
│   ├── obligations/   # ObligationTable, status badges, ReminderEmailModal (AI)
│   ├── documents/     # DocumentUploadZone (drag & drop), DocumentTable, AskDocumentsPanel (RAG)
│   ├── users/         # UserFormModal
│   └── ai/            # AiAssistant (floating chat + extraction), ChatMessageBubble
├── pages/             # Login, Dashboard, Clients, Obligations, Documents, Users (ADMIN)
│   └── portal/        # σελίδες πελάτη (CLIENT): επισκόπηση, υποχρεώσεις, έγγραφα, AI, στοιχεία
├── services/          # apiClient (Axios + JWT interceptor) και ένα module ανά feature
├── hooks/             # React Query hooks, useAiChat, useAuth, useDebounce
├── context/           # AuthProvider
├── lib/               # μορφοποίηση (el-GR), labels, ΑΦΜ check, errors
└── types/             # TypeScript τύποι που αντιστοιχούν στα backend DTOs
```

---

## 🗄️ Βάση δεδομένων

Τα migrations βρίσκονται στο `backend/src/main/resources/db/migration`:

- `V1__init_core_schema.sql`: `users`, `clients` (+ `client_activity_codes`, `client_representatives`),
  `tax_obligations`, `notifications`, `documents`.
  - unique index στο ΑΦΜ, functional unique index στο `lower(email)`
  - indexes ημερομηνιών λήξης: `(due_date)`, `(status, due_date)`, `(client_id, due_date)` και
    partial index μόνο για τις ανοιχτές υποχρεώσεις, που χρησιμοποιεί ο scheduler
  - trigram GIN index (`pg_trgm`) για ILIKE αναζήτηση στην επωνυμία
  - CHECK constraints για enums και κανόνες (π.χ. `SUBMITTED ⇒ submitted_at`)
- `V2__rag_vector_store.sql`: `document_chunks` με `vector(768)` και HNSW index (cosine).
- `V3__client_portal_accounts.sql`: ρόλος `CLIENT` και `users.client_id`. Ένας CHECK εξασφαλίζει ότι κάθε
  λογαριασμός πελάτη δένεται με έναν πελάτη και κανένας λογαριασμός προσωπικού με κανέναν.
- `V4__obligation_messages.sql`: `obligation_messages` (συζήτηση λογιστή και πελάτη ανά υποχρέωση) και οι τύποι ειδοποιήσεων
  `MESSAGE`, `STATUS_CHANGED`, `DOCUMENT_RECEIVED`.

Το Hibernate τρέχει με `ddl-auto: validate`. Το σχήμα ανήκει αποκλειστικά στο Flyway.

---

## 🔌 REST API (`/api/v1`)

| Method | Endpoint | Ρόλοι |
|---|---|---|
| POST | `/auth/login` · GET `/auth/me` | δημόσιο / όλοι |
| GET/POST/PUT | `/users` | ανάγνωση: όλοι · εγγραφή: ADMIN |
| GET | `/clients?q=&type=&bookCategory=&active=&page=&size=&sort=` | όλοι |
| GET | `/clients/{id}` · `/clients/by-afm/{afm}` | όλοι |
| POST/PUT · DELETE | `/clients` · `/clients/{id}` | ADMIN, ACCOUNTANT · ADMIN |
| GET | `/obligations?clientId=&status=&type=&dueFrom=&dueTo=&mine=` | όλοι |
| POST/PUT · DELETE | `/obligations` · `/obligations/{id}` | ADMIN, ACCOUNTANT · ADMIN |
| PATCH | `/obligations/{id}/status` | όλοι |
| POST | `/obligations/reminders/run` | ADMIN |
| GET/PATCH/POST | `/notifications`, `/notifications/unread-count`, `/{id}/read`, `/read-all` | όλοι (μόνο τις δικές τους) |
| POST/GET | `/clients/{id}/documents` · `/documents/{id}[/download\|/reindex]` | όλοι · reindex/delete: ADMIN, ACCOUNTANT |
| GET | `/dashboard/stats` | όλοι |
| POST | `/ai/chat` · `/ai/extract` · `/ai/reminder-email` · `/ai/documents/ask` | όλοι |
| GET | `/ai/health` | όλοι |
| GET · POST/PUT | `/clients/{id}/portal-accounts[/{userId}]` | όλοι · ADMIN, ACCOUNTANT |
| GET | `/portal/profile` · `/portal/obligations?status=` · `/portal/documents` | CLIENT |
| POST/GET | `/portal/documents` · `/portal/documents/{id}/download` | CLIENT |
| POST | `/portal/ai/ask` | CLIENT |
| GET/POST | `/obligations/{id}/messages` · `/portal/obligations/{id}/messages` | όλοι · CLIENT (μόνο δικές του) |

Στον παραπάνω πίνακα το «όλοι» σημαίνει όλο το προσωπικό. Ο ρόλος `CLIENT` έχει πρόσβαση μόνο στο `/portal/**`,
στο `/auth/me` και στις δικές του ειδοποιήσεις.

Η πλήρης τεκμηρίωση είναι στο Swagger UI.

### Workflow υποχρεώσεων

```
PENDING_DOCS ⇄ IN_PROGRESS ──► SUBMITTED
      └───────────┴── (παρέλευση προθεσμίας) ──► OVERDUE ──► SUBMITTED (εκπρόθεσμη υποβολή)
SUBMITTED ──► IN_PROGRESS   (επαναφορά για τροποποιητική· αν έχει λήξει γίνεται αμέσως OVERDUE)
```

Το `OVERDUE` το ορίζει μόνο το σύστημα. Μια παράταση προθεσμίας επαναφέρει μια εκπρόθεσμη υποχρέωση σε `IN_PROGRESS`.

### Αυτόματες υπενθυμίσεις

Το `DeadlineReminderScheduler` τρέχει καθημερινά στις 07:00 (Europe/Athens) και μία φορά κατά την εκκίνηση, για
να καλύπτει downtime. Επισημαίνει τις εκπρόθεσμες υποχρεώσεις και στέλνει υπενθυμίσεις D-7 / D-3 / D-1 στον
υπεύθυνο, αλλιώς στον λογιστή του πελάτη, αλλιώς στους διαχειριστές. Κάθε ειδοποίηση έχει deterministic
`dedup_key` και εισάγεται με `ON CONFLICT DO NOTHING`, άρα η επανεκτέλεση είναι πάντα ασφαλής. Το email channel
ενεργοποιείται με `NOTIFICATIONS_EMAIL_ENABLED=true` και στέλνει μόνο μετά το commit.

---

## 🤖 AI Copilot (τοπικό Ollama)

`OllamaIntegrationService` (Spring `RestClient`, timeouts, μετάφραση σφαλμάτων):

| Ollama endpoint | Χρήση |
|---|---|
| `POST /api/chat` | συνομιλία, με προαιρετικό context πελάτη ή εγγράφων |
| `POST /api/generate` | single-shot εργασίες (εξαγωγή, email) |
| `POST /api/embed` | embeddings για RAG |
| `GET /api/tags` | health check / διαθεσιμότητα μοντέλων |

**Structured JSON output**: οι μέθοδοι `chatStructured` / `generateStructured` στέλνουν JSON Schema στο πεδίο
`format`, οπότε το Ollama περιορίζει την παραγωγή ακριβώς σε αυτό το σχήμα και η απάντηση αντιστοιχίζεται απευθείας
σε Java record.

Use cases:
1. **Εξαγωγή δεδομένων** (`/ai/extract`): από ελεύθερο κείμενο προκύπτουν εγγραφές με ΑΦΜ, ποσό, ημερομηνία, τύπο
   υποχρέωσης κ.λπ. Η έξοδος του LLM θεωρείται μη αξιόπιστη: ελέγχεται το check digit του ΑΦΜ, οι ημερομηνίες και
   τα ποσά αναλύονται αυστηρά, γίνεται αντιστοίχιση με υπάρχοντα πελάτη και επιστρέφονται warnings.
2. **Email υπενθύμισης** (`/ai/reminder-email`): προσωποποιημένο προσχέδιο (subject/body) σε επιλεγμένο ύφος.
   Δεν αποστέλλεται αυτόματα. Ο λογιστής το ελέγχει και το στέλνει ο ίδιος.
3. **RAG** (`/ai/documents/ask` ή `useDocuments` στο chat): upload → εξαγωγή κειμένου (PDF/TXT) → chunking με
   επικάλυψη → embeddings → pgvector. Η αναζήτηση γίνεται με cosine similarity ανά πελάτη, και οι απαντήσεις
   παραπέμπουν στις πηγές τους ως `[n]`.

Μέτρα ασφάλειας: το κείμενο του χρήστη μπαίνει πάντα μέσα σε delimiters και τα system prompts δηλώνουν ρητά ότι
είναι δεδομένα και όχι οδηγίες. Ο ρόλος `system` δεν γίνεται δεκτός από τον client.

Αλλαγή μοντέλου: ορίστε `OLLAMA_CHAT_MODEL=qwen2.5-coder` (ή άλλο) στο `.env` και επανεκκινήστε. Το
`ollama-init` θα κατεβάσει το νέο μοντέλο.

---

## 🔐 Ασφάλεια

- Stateless JWT (HS256, secret ≥ 32 bytes, υποχρεωτικό στο compose), BCrypt(12), RBAC με `@PreAuthorize`
- Σε κάθε αίτημα ελέγχεται η τρέχουσα κατάσταση και ο ρόλος του χρήστη (cache 30″, άμεσο evict στις αλλαγές):
  η απενεργοποίηση ή η αλλαγή ρόλου ισχύει αμέσως, χωρίς να περιμένει τη λήξη του token
- Ρόλοι: `ADMIN` (χρήστες, διαγραφές), `ACCOUNTANT` (πλήρης διαχείριση), `ASSISTANT` (ανάγνωση και workflow),
  `CLIENT` (portal πελάτη)
- Portal πελάτη: ο λογαριασμός `CLIENT` περιορίζεται από τους κανόνες URL στο `/api/v1/portal/**`. Όλο το υπόλοιπο API
  είναι μόνο για το προσωπικό, οπότε ένα νέο endpoint δεν εκτίθεται κατά λάθος σε πελάτες. Ο πελάτης προκύπτει από
  τη βάση σε κάθε αίτημα και δεν διαβάζεται ποτέ από το αίτημα. Οι απαντήσεις του portal δεν περιέχουν εσωτερικές
  σημειώσεις ή αναθέσεις
- Uploads: whitelist τύπων, έλεγχος magic bytes για PDF, SHA-256 de-duplication, προστασία από path traversal,
  atomic εγγραφή και καθαρισμός αρχείων σε rollback
- nginx: CSP, `X-Frame-Options: DENY`, `nosniff`, `no-referrer`
- Περιορισμός page size (100) και μηνύματα σφαλμάτων που δεν διαρρέουν SQL ή stack traces

### Προτεινόμενα επόμενα βήματα για production

- Refresh tokens σε httpOnly cookie (σήμερα το access token φυλάσσεται στο `localStorage`)
- ShedLock για τον scheduler όταν τρέχουν πολλά instances (το job είναι ήδη idempotent)
- OCR (π.χ. Tesseract) για σαρωμένα PDF στο `DocumentTextExtractor`
- Streaming απαντήσεων chat (SSE) και multi-tenancy (`office_id`) αν εξυπηρετούνται πολλά γραφεία
- Αυτόματο import προθεσμιών από το φορολογικό ημερολόγιο της ΑΑΔΕ

## Άδεια

MIT. Δείτε το [LICENSE](LICENSE).
