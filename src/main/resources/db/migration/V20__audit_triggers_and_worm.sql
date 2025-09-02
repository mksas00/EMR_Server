-- Audyt zmian i WORM dla logów audytowych
set search_path to emr, public;

-- Funkcja audytująca modyfikacje danych
CREATE OR REPLACE FUNCTION emr.audit_change()
RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_user uuid := emr.current_user_uuid();
    v_action text := TG_OP;
    v_patient uuid;
    v_desc text := TG_TABLE_NAME || ' change';
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF TG_ARGV[0] IS NOT NULL AND TG_ARGV[0] <> '' THEN
            EXECUTE 'SELECT ($1).' || TG_ARGV[0] INTO v_patient USING NEW;
        END IF;
        INSERT INTO emr.audit_log(user_id, patient_id, action, description)
        VALUES (v_user, v_patient, v_action, v_desc);
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        IF TG_ARGV[0] IS NOT NULL AND TG_ARGV[0] <> '' THEN
            EXECUTE 'SELECT ($1).' || TG_ARGV[0] INTO v_patient USING NEW;
        END IF;
        INSERT INTO emr.audit_log(user_id, patient_id, action, description)
        VALUES (v_user, v_patient, v_action, v_desc);
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        IF TG_ARGV[0] IS NOT NULL AND TG_ARGV[0] <> '' THEN
            EXECUTE 'SELECT ($1).' || TG_ARGV[0] INTO v_patient USING OLD;
        END IF;
        INSERT INTO emr.audit_log(user_id, patient_id, action, description)
        VALUES (v_user, v_patient, v_action, v_desc);
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$;

-- Trigger helper: tworzy trigger na tabeli, z kolumną wskazującą na patient_id
-- Użyjemy poniżej przez CREATE TRIGGER bez helpera (Postgres nie wspiera makr)

-- Tabele powiązane z pacjentem (argument 1 = nazwa kolumny patient_id lub 'id' dla patients)
DROP TRIGGER IF EXISTS trg_audit_patients ON emr.patients;
CREATE TRIGGER trg_audit_patients
AFTER INSERT OR UPDATE OR DELETE ON emr.patients
FOR EACH ROW EXECUTE FUNCTION emr.audit_change('id');

DROP TRIGGER IF EXISTS trg_audit_medical_records ON emr.medical_records;
CREATE TRIGGER trg_audit_medical_records
AFTER INSERT OR UPDATE OR DELETE ON emr.medical_records
FOR EACH ROW EXECUTE FUNCTION emr.audit_change('patient_id');

DROP TRIGGER IF EXISTS trg_audit_lab_results ON emr.lab_results;
CREATE TRIGGER trg_audit_lab_results
AFTER INSERT OR UPDATE OR DELETE ON emr.lab_results
FOR EACH ROW EXECUTE FUNCTION emr.audit_change('patient_id');

DROP TRIGGER IF EXISTS trg_audit_prescriptions ON emr.prescriptions;
CREATE TRIGGER trg_audit_prescriptions
AFTER INSERT OR UPDATE OR DELETE ON emr.prescriptions
FOR EACH ROW EXECUTE FUNCTION emr.audit_change('patient_id');

DROP TRIGGER IF EXISTS trg_audit_visits ON emr.visits;
CREATE TRIGGER trg_audit_visits
AFTER INSERT OR UPDATE OR DELETE ON emr.visits
FOR EACH ROW EXECUTE FUNCTION emr.audit_change('patient_id');

DROP TRIGGER IF EXISTS trg_audit_medical_files ON emr.medical_files;
CREATE TRIGGER trg_audit_medical_files
AFTER INSERT OR UPDATE OR DELETE ON emr.medical_files
FOR EACH ROW EXECUTE FUNCTION emr.audit_change('patient_id');

DROP TRIGGER IF EXISTS trg_audit_allergies ON emr.allergies;
CREATE TRIGGER trg_audit_allergies
AFTER INSERT OR UPDATE OR DELETE ON emr.allergies
FOR EACH ROW EXECUTE FUNCTION emr.audit_change('patient_id');

DROP TRIGGER IF EXISTS trg_audit_chronic_diseases ON emr.chronic_diseases;
CREATE TRIGGER trg_audit_chronic_diseases
AFTER INSERT OR UPDATE OR DELETE ON emr.chronic_diseases
FOR EACH ROW EXECUTE FUNCTION emr.audit_change('patient_id');

DROP TRIGGER IF EXISTS trg_audit_medication_history ON emr.medication_history;
CREATE TRIGGER trg_audit_medication_history
AFTER INSERT OR UPDATE OR DELETE ON emr.medication_history
FOR EACH ROW EXECUTE FUNCTION emr.audit_change('patient_id');

-- WORM: zablokuj modyfikacje audit_log
CREATE OR REPLACE FUNCTION emr.audit_log_protect()
RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER
AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is WORM: % not allowed', TG_OP USING ERRCODE = 'insufficient_privilege';
END;
$$;

DROP TRIGGER IF EXISTS trg_protect_audit_log_upd ON emr.audit_log;
CREATE TRIGGER trg_protect_audit_log_upd
BEFORE UPDATE OR DELETE ON emr.audit_log
FOR EACH ROW EXECUTE FUNCTION emr.audit_log_protect();

