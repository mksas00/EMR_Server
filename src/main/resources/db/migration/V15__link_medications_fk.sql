-- Link prescriptions and medication_history to medications via optional FK
ALTER TABLE emr.prescriptions ADD COLUMN IF NOT EXISTS medication_id uuid REFERENCES emr.medications(id);
ALTER TABLE emr.medication_history ADD COLUMN IF NOT EXISTS medication_id uuid REFERENCES emr.medications(id);

-- Optional helpful index
CREATE INDEX IF NOT EXISTS prescriptions_medication_id_idx ON emr.prescriptions(medication_id);
CREATE INDEX IF NOT EXISTS medhist_medication_id_idx ON emr.medication_history(medication_id);

