-- Admin role is a real, DB-backed flag (not derived from the username).
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- A room can be replayed (rematch), so the same room_code may finish several
-- times. Drop the UNIQUE constraint so every finished round is its own row.
ALTER TABLE games DROP CONSTRAINT IF EXISTS games_room_code_key;
