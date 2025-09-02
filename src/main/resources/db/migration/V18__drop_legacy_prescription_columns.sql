-- Drop legacy columns from prescriptions (moved to prescription_medications)
ALTER TABLE emr.prescriptions
    DROP COLUMN IF EXISTS medication_id,
    DROP COLUMN IF EXISTS medication;

