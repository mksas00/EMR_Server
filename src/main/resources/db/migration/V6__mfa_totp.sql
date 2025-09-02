-- V6: MFA TOTP + recovery codes
ALTER TABLE emr.users ADD COLUMN IF NOT EXISTS mfa_enabled boolean DEFAULT false;

CREATE TABLE IF NOT EXISTS emr.mfa_recovery_codes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES emr.users(id) ON DELETE CASCADE,
    code_hash varchar(64) NOT NULL,
    used_at timestamptz,
    created_at timestamptz DEFAULT now()
);
CREATE INDEX IF NOT EXISTS mfa_recovery_codes_user_idx ON emr.mfa_recovery_codes(user_id) WHERE used_at IS NULL;

