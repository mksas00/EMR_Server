-- BTG: dodanie wygasania zgody i triggera audytu
set search_path to emr, public;

ALTER TABLE emr.patient_consents
  ADD COLUMN IF NOT EXISTS expires_at timestamptz;

ALTER TABLE emr.patient_consents
  ADD CONSTRAINT patient_consents_expires_after_granted
  CHECK (expires_at IS NULL OR expires_at > granted_at);

-- Aktualizacja funkcji can_access_patient, by uwzględnić wygaśnięcie zgody
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
    -- Zgoda pacjenta (aktywna i nieprzedawniona)
    IF EXISTS (
        SELECT 1 FROM emr.patient_consents c
        WHERE c.patient_id = p_patient_id
          AND c.granted_to_user_id = cur_user
          AND c.revoked_at IS NULL
          AND (c.expires_at IS NULL OR c.expires_at > now())
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

-- Audyt zmian zgód (INSERT/UPDATE/DELETE)
DROP TRIGGER IF EXISTS trg_audit_patient_consents ON emr.patient_consents;
CREATE TRIGGER trg_audit_patient_consents
AFTER INSERT OR UPDATE OR DELETE ON emr.patient_consents
FOR EACH ROW EXECUTE FUNCTION emr.audit_change('patient_id');

