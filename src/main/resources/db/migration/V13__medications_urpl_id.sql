-- Add URPL ID to medications for stable mapping
ALTER TABLE emr.medications ADD COLUMN IF NOT EXISTS urpl_id bigint;
CREATE UNIQUE INDEX IF NOT EXISTS medications_urpl_id_uq ON emr.medications(urpl_id) WHERE urpl_id IS NOT NULL;

