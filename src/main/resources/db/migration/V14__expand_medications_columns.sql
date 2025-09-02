-- Expand columns to avoid length overflows from URPL data
ALTER TABLE emr.medications
    ALTER COLUMN strength TYPE text,
    ALTER COLUMN pharmaceutical_form TYPE text;

