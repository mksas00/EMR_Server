-- V5: zapewnienie unikalności zaszyfrowanego pola PESEL po przejściu na deterministyczne szyfrowanie
-- Jeśli constraint już istnieje, blok DO nic nie zmieni.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_namespace n ON n.oid = c.connamespace
        WHERE c.conrelid = 'emr.patients'::regclass
          AND c.conname = 'patients_pesel_uq'
    ) THEN
        ALTER TABLE emr.patients ADD CONSTRAINT patients_pesel_uq UNIQUE(pesel);
    END IF;
END$$;

