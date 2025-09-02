-- RLS: funkcje kontekstu i polityki dostępu
set search_path to emr, public;

-- Funkcje pomocnicze do odczytu GUC ustawianych przez aplikację
CREATE OR REPLACE FUNCTION emr.current_user_uuid()
RETURNS uuid
LANGUAGE sql STABLE PARALLEL SAFE
AS $$
  SELECT NULLIF(current_setting('emr.current_user_id', true), '')::uuid;
$$;

CREATE OR REPLACE FUNCTION emr.current_user_role()
RETURNS text
LANGUAGE sql STABLE PARALLEL SAFE
AS $$
  SELECT NULLIF(current_setting('emr.current_user_role', true), '');
$$;

-- Funkcja sprawdzająca dostęp do pacjenta
CREATE OR REPLACE FUNCTION emr.can_access_patient(p_patient_id uuid)
RETURNS boolean
LANGUAGE plpgsql STABLE SECURITY INVOKER
AS $$
DECLARE
    cur_user uuid := emr.current_user_uuid();
    cur_role text := coalesce(emr.current_user_role(), '');
BEGIN
    IF cur_role = 'admin' THEN
        RETURN true;
    END IF;
    IF cur_user IS NULL THEN
        RETURN false;
    END IF;
    -- Zgoda pacjenta
    IF EXISTS (
        SELECT 1 FROM emr.patient_consents c
        WHERE c.patient_id = p_patient_id
          AND c.granted_to_user_id = cur_user
          AND c.revoked_at IS NULL
    ) THEN
        RETURN true;
    END IF;
    -- Lekarz prowadzący (na podstawie wizyt)
    IF EXISTS (
        SELECT 1 FROM emr.visits v
        WHERE v.patient_id = p_patient_id
          AND v.doctor_id = cur_user
    ) THEN
        RETURN true;
    END IF;
    RETURN false;
END;
$$;

-- Włączenie RLS i polityki dla tabel pacjenta i powiązanych
-- Patients
ALTER TABLE emr.patients ENABLE ROW LEVEL SECURITY;
CREATE POLICY patients_read ON emr.patients FOR SELECT USING (emr.can_access_patient(id));
CREATE POLICY patients_write ON emr.patients FOR ALL USING (emr.current_user_role() IN ('admin','doctor'));

-- Medical records
ALTER TABLE emr.medical_records ENABLE ROW LEVEL SECURITY;
CREATE POLICY medrec_read ON emr.medical_records FOR SELECT USING (emr.can_access_patient(patient_id));
CREATE POLICY medrec_write ON emr.medical_records FOR ALL USING (emr.current_user_role() IN ('admin','doctor'));

-- Lab results
ALTER TABLE emr.lab_results ENABLE ROW LEVEL SECURITY;
CREATE POLICY lab_read ON emr.lab_results FOR SELECT USING (emr.can_access_patient(patient_id));
CREATE POLICY lab_write ON emr.lab_results FOR ALL USING (emr.current_user_role() IN ('admin','doctor'));

-- Prescriptions
ALTER TABLE emr.prescriptions ENABLE ROW LEVEL SECURITY;
CREATE POLICY rx_read ON emr.prescriptions FOR SELECT USING (emr.can_access_patient(patient_id));
CREATE POLICY rx_write ON emr.prescriptions FOR ALL USING (emr.current_user_role() IN ('admin','doctor'));

-- Visits
ALTER TABLE emr.visits ENABLE ROW LEVEL SECURITY;
CREATE POLICY visits_read ON emr.visits FOR SELECT USING (emr.can_access_patient(patient_id));
CREATE POLICY visits_write ON emr.visits FOR ALL USING (emr.current_user_role() IN ('admin','doctor'));

-- Files
ALTER TABLE emr.medical_files ENABLE ROW LEVEL SECURITY;
CREATE POLICY files_read ON emr.medical_files FOR SELECT USING (emr.can_access_patient(patient_id));
CREATE POLICY files_write ON emr.medical_files FOR ALL USING (emr.current_user_role() IN ('admin','doctor'));

-- Allergies
ALTER TABLE emr.allergies ENABLE ROW LEVEL SECURITY;
CREATE POLICY allergies_read ON emr.allergies FOR SELECT USING (emr.can_access_patient(patient_id));
CREATE POLICY allergies_write ON emr.allergies FOR ALL USING (emr.current_user_role() IN ('admin','doctor'));

-- Chronic diseases
ALTER TABLE emr.chronic_diseases ENABLE ROW LEVEL SECURITY;
CREATE POLICY chronic_read ON emr.chronic_diseases FOR SELECT USING (emr.can_access_patient(patient_id));
CREATE POLICY chronic_write ON emr.chronic_diseases FOR ALL USING (emr.current_user_role() IN ('admin','doctor'));

-- Medication history
ALTER TABLE emr.medication_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY medhist_read ON emr.medication_history FOR SELECT USING (emr.can_access_patient(patient_id));
CREATE POLICY medhist_write ON emr.medication_history FOR ALL USING (emr.current_user_role() IN ('admin','doctor'));

-- Sesje użytkownika: tylko właściciel lub admin
ALTER TABLE emr.user_sessions ENABLE ROW LEVEL SECURITY;
CREATE POLICY sessions_self ON emr.user_sessions FOR SELECT USING (
    emr.current_user_role() = 'admin' OR emr.current_user_uuid() = user_id
);

