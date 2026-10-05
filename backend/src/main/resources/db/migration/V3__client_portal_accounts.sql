-- =====================================================================
-- Client portal: login accounts for the office's clients.
-- A CLIENT user is bound to exactly one client and sees only that client's data;
-- staff users are never bound to a client.
-- =====================================================================

ALTER TABLE users ADD COLUMN client_id UUID REFERENCES clients (id) ON DELETE CASCADE;

ALTER TABLE users DROP CONSTRAINT chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'ACCOUNTANT', 'ASSISTANT', 'CLIENT'));
ALTER TABLE users ADD CONSTRAINT chk_users_client_binding CHECK ((role = 'CLIENT') = (client_id IS NOT NULL));

CREATE INDEX ix_users_client ON users (client_id) WHERE client_id IS NOT NULL;
