-- Add status and end_date to visits, and create visit_slots table
ALTER TABLE emr.visits
    ADD COLUMN IF NOT EXISTS end_date timestamptz NULL,
    ADD COLUMN IF NOT EXISTS status varchar(20) NOT NULL DEFAULT 'PLANNED';

-- Basic index to speed up overlap queries
CREATE INDEX IF NOT EXISTS visits_doctor_date_idx ON emr.visits(doctor_id, visit_date, end_date);

-- Visit slots for scheduling
CREATE TABLE IF NOT EXISTS emr.visit_slots (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id uuid NOT NULL REFERENCES emr.users(id) ON DELETE SET NULL,
    start_time timestamptz NOT NULL,
    end_time timestamptz NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'AVAILABLE',
    patient_id uuid NULL REFERENCES emr.patients(id) ON DELETE SET NULL,
    visit_id uuid NULL REFERENCES emr.visits(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS visit_slots_doctor_idx ON emr.visit_slots(doctor_id, start_time, end_time);
CREATE INDEX IF NOT EXISTS visit_slots_status_idx ON emr.visit_slots(status);

-- Ensure logical constraints
ALTER TABLE emr.visit_slots
    ADD CONSTRAINT visit_slots_time_chk CHECK (start_time < end_time);

-- Optional: prevent overlapping AVAILABLE slots for the same doctor (soft, not enforced here)

