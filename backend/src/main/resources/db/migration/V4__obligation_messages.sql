-- =====================================================================
-- Accountant <-> client conversation, one thread per obligation, and the
-- notification types that keep both sides informed.
-- =====================================================================

CREATE TABLE obligation_messages (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    obligation_id UUID         NOT NULL REFERENCES tax_obligations (id) ON DELETE CASCADE,
    author_id     UUID         REFERENCES users (id) ON DELETE SET NULL,
    author_name   VARCHAR(150) NOT NULL,           -- kept even if the account is removed
    from_client   BOOLEAN      NOT NULL,           -- written from the client portal
    body          TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_obl_msg_body CHECK (length(btrim(body)) BETWEEN 1 AND 4000)
);
CREATE INDEX ix_obl_msg_obligation ON obligation_messages (obligation_id, created_at);

ALTER TABLE notifications DROP CONSTRAINT chk_notif_type;
ALTER TABLE notifications ADD CONSTRAINT chk_notif_type CHECK (type IN
    ('DEADLINE_UPCOMING', 'DEADLINE_OVERDUE', 'DOCUMENT_PROCESSED', 'SYSTEM',
     'MESSAGE', 'STATUS_CHANGED', 'DOCUMENT_RECEIVED'));
