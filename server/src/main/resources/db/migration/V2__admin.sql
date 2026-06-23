-- Stage 5: administration support.
-- Flag used by the admin panel to block players; checked on login.
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_blocked BOOLEAN NOT NULL DEFAULT FALSE;
