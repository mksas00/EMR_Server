-- Medications and packages (URPL mapping)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS emr.medications (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,                                   -- Nazwa Produktu Leczniczego
    common_name text,                                     -- Nazwa powszechnie stosowana (INN)
    banned_for_animals boolean,                           -- Zakaz stosowania u zwierząt
    strength varchar(200),                                -- Moc
    pharmaceutical_form varchar(300),                     -- Postać farmaceutyczna
    authorization_number varchar(100),                    -- Nr pozwolenia
    authorization_valid_to date,                          -- Ważność pozwolenia (do)
    marketing_authorization_holder varchar(400),          -- Podmiot odpowiedzialny
    procedure_type varchar(100),                          -- Typ procedury
    legal_basis varchar(200),                             -- Podstawa prawna wniosku
    atc_code varchar(20),                                 -- Kod ATC
    active_substances text,                               -- Substancja czynna (tekst; opcjonalnie JSON w przyszłości)
    target_species text,                                  -- Gatunki docelowe
    packaging_consent text,                               -- Opakowanie - Zgody Prezesa (tekst surowy)
    prescription_category varchar(50),                    -- Kat. dost.
    effective_from date,
    effective_to date,
    created_at timestamptz DEFAULT now()
);
CREATE INDEX IF NOT EXISTS medications_name_trgm_idx ON emr.medications USING gin (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS medications_atc_idx ON emr.medications(atc_code);
CREATE INDEX IF NOT EXISTS medications_auth_no_idx ON emr.medications(authorization_number);

CREATE TABLE IF NOT EXISTS emr.medication_packages (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    medication_id uuid NOT NULL REFERENCES emr.medications(id) ON DELETE CASCADE,
    gtin varchar(14),                                     -- Numer GTIN
    pack_description text,                                -- Surowy opis opakowania (brak struktury w URPL)
    supply_status varchar(100),                           -- Dostępność (opcjonalnie)
    effective_from date,
    effective_to date,
    created_at timestamptz DEFAULT now(),
    CONSTRAINT medication_packages_gtin_uq UNIQUE (gtin)
);
CREATE INDEX IF NOT EXISTS medication_packages_med_idx ON emr.medication_packages(medication_id);
CREATE INDEX IF NOT EXISTS medication_packages_eff_idx ON emr.medication_packages(medication_id, effective_from DESC);
