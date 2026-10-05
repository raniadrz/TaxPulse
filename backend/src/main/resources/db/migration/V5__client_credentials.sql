-- =====================================================================
-- Client credentials for public services (TAXISnet, e-ΕΦΚΑ, ...), kept so the
-- office can file on the client's behalf. Passwords are AES-256-GCM encrypted
-- by the application; the database never sees them in clear. Every reveal and
-- change is written to credential_access_log.
-- =====================================================================

CREATE TABLE client_credentials (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id          UUID         NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    kind               VARCHAR(20)  NOT NULL,
    label              VARCHAR(100),
    username           VARCHAR(255) NOT NULL,
    password_encrypted TEXT         NOT NULL,          -- base64(iv || ciphertext || tag)
    updated_by_id      UUID         REFERENCES users (id) ON DELETE SET NULL,
    updated_by_name    VARCHAR(150),
    updated_by_client  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_cred_kind CHECK (kind IN ('TAXISNET', 'EFKA', 'OTHER'))
);
CREATE INDEX ix_cred_client ON client_credentials (client_id);
-- One TAXISnet / one e-ΕΦΚΑ login per client; "OTHER" may repeat.
CREATE UNIQUE INDEX ux_cred_client_kind ON client_credentials (client_id, kind) WHERE kind <> 'OTHER';

CREATE TABLE credential_access_log (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id     UUID         NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    credential_id UUID,                                -- kept after the credential is deleted
    kind          VARCHAR(20)  NOT NULL,
    user_id       UUID         REFERENCES users (id) ON DELETE SET NULL,
    user_name     VARCHAR(150) NOT NULL,
    by_client     BOOLEAN      NOT NULL,
    action        VARCHAR(10)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_cred_log_action CHECK (action IN ('VIEW', 'CREATE', 'UPDATE', 'DELETE'))
);
CREATE INDEX ix_cred_log_client ON credential_access_log (client_id, created_at DESC);

ALTER TABLE notifications DROP CONSTRAINT chk_notif_type;
ALTER TABLE notifications ADD CONSTRAINT chk_notif_type CHECK (type IN
    ('DEADLINE_UPCOMING', 'DEADLINE_OVERDUE', 'DOCUMENT_PROCESSED', 'SYSTEM',
     'MESSAGE', 'STATUS_CHANGED', 'DOCUMENT_RECEIVED', 'CREDENTIALS_UPDATED'));
