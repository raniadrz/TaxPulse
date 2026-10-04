-- =====================================================================
-- TaxPulse AI - Core schema
-- ---------------------------------------------------------------------
-- Conventions
--   * UUID primary keys (gen_random_uuid() is built into PostgreSQL 13+).
--   * Enumerations are stored as VARCHAR + CHECK constraint instead of
--     native PG ENUMs: adding a value is a trivial migration and it maps
--     cleanly to JPA @Enumerated(EnumType.STRING).
--   * Every mutable table carries created_at / updated_at and an
--     optimistic-locking "version" column used by JPA @Version.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- fuzzy / ILIKE search on names

-- ---------------------------------------------------------------------
-- users: accountants and office staff (application principals)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,              -- BCrypt hash
    full_name      VARCHAR(150) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'ACCOUNTANT', 'ASSISTANT'))
);
-- Case-insensitive uniqueness for login e-mails.
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

-- ---------------------------------------------------------------------
-- clients: natural persons (Φυσικά Πρόσωπα) and legal entities (Νομικά)
-- ---------------------------------------------------------------------
CREATE TABLE clients (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    client_type            VARCHAR(20)  NOT NULL,
    afm                    CHAR(9)      NOT NULL,          -- ΑΦΜ (Greek VAT number)
    doy                    VARCHAR(100) NOT NULL,          -- ΔΟΥ (tax office)
    -- Εταιρική επωνυμία (legal entity) or Ονοματεπώνυμο (natural person)
    name                   VARCHAR(255) NOT NULL,
    trade_name             VARCHAR(255),                   -- Διακριτικός τίτλος
    legal_form             VARCHAR(30),                    -- ΑΕ, ΕΠΕ, ΙΚΕ, ΟΕ, ΕΕ ...
    book_category          VARCHAR(10)  NOT NULL,          -- Κατηγορία βιβλίων
    gemi_number            VARCHAR(20),                    -- Αριθμός ΓΕΜΗ
    email                  VARCHAR(255),
    phone                  VARCHAR(30),
    mobile                 VARCHAR(30),
    address_street         VARCHAR(255),
    address_city           VARCHAR(100),
    address_postal_code    VARCHAR(10),
    assigned_accountant_id UUID REFERENCES users (id) ON DELETE SET NULL,
    notes                  TEXT,
    active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ux_clients_afm UNIQUE (afm),                -- also serves as the ΑΦΜ lookup index
    CONSTRAINT chk_clients_afm_format CHECK (afm ~ '^[0-9]{9}$'),
    CONSTRAINT chk_clients_type CHECK (client_type IN ('INDIVIDUAL', 'LEGAL_ENTITY')),
    CONSTRAINT chk_clients_books CHECK (book_category IN ('NONE', 'A', 'B', 'C'))
);
CREATE INDEX ix_clients_assigned_accountant ON clients (assigned_accountant_id);
-- Trigram index makes "name ILIKE '%term%'" quick searches index-backed.
CREATE INDEX ix_clients_name_trgm ON clients USING gin (name gin_trgm_ops);

-- Activity codes (ΚΑΔ). A client has exactly one primary and many secondary codes.
CREATE TABLE client_activity_codes (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID        NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    code        VARCHAR(12) NOT NULL,           -- e.g. 69201001 / 69.20.10.01
    description VARCHAR(255),
    is_primary  BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT ux_client_kad UNIQUE (client_id, code)
);
-- At most one primary ΚΑΔ per client.
CREATE UNIQUE INDEX ux_client_kad_primary ON client_activity_codes (client_id) WHERE is_primary;
CREATE INDEX ix_client_kad_code ON client_activity_codes (code);

-- Representatives (Εκπρόσωποι / Διαχειριστές) of a client.
CREATE TABLE client_representatives (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id  UUID         NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    full_name  VARCHAR(150) NOT NULL,
    afm        CHAR(9),
    role       VARCHAR(100),                    -- e.g. Διαχειριστής, Νόμιμος Εκπρόσωπος
    email      VARCHAR(255),
    phone      VARCHAR(30),
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_rep_afm_format CHECK (afm IS NULL OR afm ~ '^[0-9]{9}$')
);
CREATE INDEX ix_client_rep_client ON client_representatives (client_id);

-- ---------------------------------------------------------------------
-- tax_obligations: deadlines / filings per client (Φορολογικές υποχρεώσεις)
-- ---------------------------------------------------------------------
CREATE TABLE tax_obligations (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id        UUID          NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    obligation_type  VARCHAR(30)   NOT NULL,
    title            VARCHAR(255)  NOT NULL,
    description      TEXT,
    period_start     DATE,                       -- tax period covered (e.g. Q1 VAT)
    period_end       DATE,
    due_date         DATE          NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING_DOCS',
    amount           NUMERIC(14,2),              -- payable / refundable amount, if known
    assigned_to_id   UUID REFERENCES users (id) ON DELETE SET NULL,
    submitted_at     TIMESTAMPTZ,
    submission_ref   VARCHAR(100),               -- αριθμός πρωτοκόλλου / ΑΑΔΕ reference
    notes            TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT chk_obl_type CHECK (obligation_type IN
        ('VAT', 'INCOME_TAX', 'APD', 'MYDATA', 'GEMI', 'ENFIA', 'WITHHOLDING_TAX', 'PAYROLL', 'INTRASTAT', 'OTHER')),
    CONSTRAINT chk_obl_status CHECK (status IN ('PENDING_DOCS', 'IN_PROGRESS', 'SUBMITTED', 'OVERDUE')),
    CONSTRAINT chk_obl_period CHECK (period_end IS NULL OR period_start IS NULL OR period_end >= period_start),
    CONSTRAINT chk_obl_submitted CHECK (status <> 'SUBMITTED' OR submitted_at IS NOT NULL)
);
CREATE INDEX ix_obl_due_date        ON tax_obligations (due_date);
CREATE INDEX ix_obl_status_due_date ON tax_obligations (status, due_date);
CREATE INDEX ix_obl_client_due_date ON tax_obligations (client_id, due_date);
CREATE INDEX ix_obl_assigned_to     ON tax_obligations (assigned_to_id);
-- Hot path for the reminder / overdue schedulers: only open obligations.
CREATE INDEX ix_obl_open_due_date   ON tax_obligations (due_date)
    WHERE status IN ('PENDING_DOCS', 'IN_PROGRESS');

-- ---------------------------------------------------------------------
-- notifications: in-app / e-mail alerts for staff
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    obligation_id     UUID         REFERENCES tax_obligations (id) ON DELETE CASCADE,
    client_id         UUID         REFERENCES clients (id) ON DELETE CASCADE,
    type              VARCHAR(30)  NOT NULL,
    channel           VARCHAR(10)  NOT NULL DEFAULT 'IN_APP',
    title             VARCHAR(255) NOT NULL,
    message           TEXT         NOT NULL,
    -- Idempotency key so a scheduler re-run never produces duplicate reminders,
    -- e.g. "DEADLINE_UPCOMING:<obligationId>:<recipientId>:D-3".
    dedup_key         VARCHAR(200),
    read_at           TIMESTAMPTZ,
    sent_at           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_notif_type CHECK (type IN ('DEADLINE_UPCOMING', 'DEADLINE_OVERDUE', 'DOCUMENT_PROCESSED', 'SYSTEM')),
    CONSTRAINT chk_notif_channel CHECK (channel IN ('IN_APP', 'EMAIL')),
    CONSTRAINT ux_notif_dedup UNIQUE (dedup_key)
);
-- "My unread notifications, newest first".
CREATE INDEX ix_notif_recipient_unread ON notifications (recipient_id, created_at DESC) WHERE read_at IS NULL;
CREATE INDEX ix_notif_recipient        ON notifications (recipient_id, created_at DESC);
CREATE INDEX ix_notif_obligation       ON notifications (obligation_id);

-- ---------------------------------------------------------------------
-- documents: files uploaded per client (optionally linked to an obligation)
-- ---------------------------------------------------------------------
CREATE TABLE documents (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id         UUID          NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    obligation_id     UUID          REFERENCES tax_obligations (id) ON DELETE SET NULL,
    uploaded_by_id    UUID          REFERENCES users (id) ON DELETE SET NULL,
    original_filename VARCHAR(255)  NOT NULL,
    content_type      VARCHAR(100)  NOT NULL,
    size_bytes        BIGINT        NOT NULL,
    storage_key       VARCHAR(500)  NOT NULL,     -- path/key inside the storage backend
    checksum_sha256   CHAR(64)      NOT NULL,
    ingestion_status  VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    ingestion_error   TEXT,
    extracted_text    TEXT,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT chk_doc_status CHECK (ingestion_status IN ('PENDING', 'PROCESSING', 'INDEXED', 'FAILED', 'SKIPPED')),
    CONSTRAINT chk_doc_size CHECK (size_bytes >= 0),
    CONSTRAINT ux_doc_storage_key UNIQUE (storage_key)
);
CREATE INDEX ix_doc_client     ON documents (client_id, created_at DESC);
CREATE INDEX ix_doc_obligation ON documents (obligation_id);
CREATE INDEX ix_doc_checksum   ON documents (client_id, checksum_sha256);
