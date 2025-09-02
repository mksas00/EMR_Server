-- V7: Password reset tokens
CREATE TABLE IF NOT EXISTS emr.password_reset_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES emr.users(id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    requested_ip varchar(45),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS password_reset_tokens_token_hash_uq ON emr.password_reset_tokens(token_hash);
CREATE INDEX IF NOT EXISTS password_reset_tokens_user_active_idx ON emr.password_reset_tokens(user_id) WHERE used_at IS NULL;

