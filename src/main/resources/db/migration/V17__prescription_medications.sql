-- Create table for prescription items (many-to-one prescription -> items)
CREATE TABLE IF NOT EXISTS emr.prescription_medications (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id uuid NOT NULL REFERENCES emr.prescriptions(id) ON DELETE CASCADE,
    medication_id uuid NULL REFERENCES emr.medications(id),
    dosage_info text NULL,
    quantity integer NULL,
    unit varchar(50) NULL
);

CREATE INDEX IF NOT EXISTS idx_presc_meds_prescription ON emr.prescription_medications(prescription_id);
CREATE INDEX IF NOT EXISTS idx_presc_meds_medication ON emr.prescription_medications(medication_id);

