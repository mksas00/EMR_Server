-- V25: Zaostrzenie modelu uprawnień (RBAC) zgodnie z zasadą minimum przywilejów
-- (ISO 27799, NIST SP 800-53 AC-5 Separation of Duties).
--
-- Cel: administrator NIE powinien rutynowo modyfikować klinicznych danych pacjenta.
-- Jego rolą jest zarządzanie kontami, audyt, nadzór nad incydentami i konfiguracja systemu.
-- Personel kliniczny (lekarz, pielęgniarka, diagnosta) modyfikuje dane medyczne
-- zgodnie z zakresem swoich obowiązków, w ramach mechanizmu zgód i RLS.
--
-- Zmiany w stosunku do V19:
--   • Recepty:                tylko 'doctor'
--   • Wizyty:                 tylko 'doctor'
--   • Dokumentacja medyczna:  tylko 'doctor'
--   • Pliki medyczne:         tylko 'doctor'
--   • Alergie:                tylko 'doctor'
--   • Choroby przewlekłe:     tylko 'doctor'
--   • Historia leków:         tylko 'doctor'
--   • Wyniki laboratoryjne:   'doctor' (zleca) + 'lab_tech' (wprowadza wyniki)
--   • Pacjenci:               'admin' (rejestracja) + 'doctor' (aktualizacja kliniczna)
--
-- Odczyt (SELECT) pozostaje bez zmian – sterowany funkcją emr.can_access_patient(),
-- która już uwzględnia admin override do celów audytu / nadzoru.

SET search_path TO emr, public;

-- ─── PACJENCI ───────────────────────────────────────────────────────────
-- Admin może rejestrować/aktualizować dane referencyjne pacjenta, doktor także.
DROP POLICY IF EXISTS patients_write ON emr.patients;
CREATE POLICY patients_write ON emr.patients
    FOR ALL USING (emr.current_user_role() IN ('admin','doctor'));

-- ─── MEDICAL RECORDS ────────────────────────────────────────────────────
DROP POLICY IF EXISTS medrec_write ON emr.medical_records;
CREATE POLICY medrec_write ON emr.medical_records
    FOR ALL USING (emr.current_user_role() = 'doctor');

-- ─── LAB RESULTS ────────────────────────────────────────────────────────
-- Diagnosta laboratoryjny wprowadza wyniki, lekarz zleca i koryguje.
DROP POLICY IF EXISTS lab_write ON emr.lab_results;
CREATE POLICY lab_write ON emr.lab_results
    FOR ALL USING (emr.current_user_role() IN ('doctor','lab_tech'));

-- ─── PRESCRIPTIONS ──────────────────────────────────────────────────────
-- Recepty wystawia wyłącznie lekarz (uprawnienie zawodowe).
DROP POLICY IF EXISTS rx_write ON emr.prescriptions;
CREATE POLICY rx_write ON emr.prescriptions
    FOR ALL USING (emr.current_user_role() = 'doctor');

-- ─── VISITS ─────────────────────────────────────────────────────────────
DROP POLICY IF EXISTS visits_write ON emr.visits;
CREATE POLICY visits_write ON emr.visits
    FOR ALL USING (emr.current_user_role() = 'doctor');

-- ─── MEDICAL FILES ──────────────────────────────────────────────────────
DROP POLICY IF EXISTS files_write ON emr.medical_files;
CREATE POLICY files_write ON emr.medical_files
    FOR ALL USING (emr.current_user_role() = 'doctor');

-- ─── ALLERGIES ──────────────────────────────────────────────────────────
DROP POLICY IF EXISTS allergies_write ON emr.allergies;
CREATE POLICY allergies_write ON emr.allergies
    FOR ALL USING (emr.current_user_role() = 'doctor');

-- ─── CHRONIC DISEASES ───────────────────────────────────────────────────
DROP POLICY IF EXISTS chronic_write ON emr.chronic_diseases;
CREATE POLICY chronic_write ON emr.chronic_diseases
    FOR ALL USING (emr.current_user_role() = 'doctor');

-- ─── MEDICATION HISTORY ─────────────────────────────────────────────────
DROP POLICY IF EXISTS medhist_write ON emr.medication_history;
CREATE POLICY medhist_write ON emr.medication_history
    FOR ALL USING (emr.current_user_role() = 'doctor');

