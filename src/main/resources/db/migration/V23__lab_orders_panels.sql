-- V23: Lab redesign – zlecenia (LabOrder), pozycje (LabOrderItem) i panele (LabPanel)
-- Klinicznie poprawny model: jedno zlecenie = wiele pozycji, możliwość użycia gotowego panelu.
-- Stara tabela lab_results pozostaje (legacy, wstecz kompatybilna).

set search_path to emr, public;

-- ====== ZLECENIA ======
CREATE TABLE IF NOT EXISTS emr.lab_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES emr.patients(id) ON DELETE CASCADE,
    ordered_by UUID NOT NULL REFERENCES emr.users(id),
    ordered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    priority VARCHAR(10) NOT NULL DEFAULT 'routine'
        CHECK (priority IN ('routine','urgent','stat')),
    status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending','in_progress','completed','cancelled')),
    notes TEXT,
    cancelled_at TIMESTAMPTZ,
    cancelled_by UUID REFERENCES emr.users(id),
    cancelled_reason TEXT,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_lab_orders_patient ON emr.lab_orders(patient_id);
CREATE INDEX IF NOT EXISTS idx_lab_orders_status ON emr.lab_orders(status);
CREATE INDEX IF NOT EXISTS idx_lab_orders_ordered_at ON emr.lab_orders(ordered_at DESC);
CREATE INDEX IF NOT EXISTS idx_lab_orders_ordered_by ON emr.lab_orders(ordered_by);

-- ====== POZYCJE ZLECENIA ======
-- test_name / result są szyfrowane przez warstwę aplikacyjną => TEXT (długość bez limitu)
CREATE TABLE IF NOT EXISTS emr.lab_order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES emr.lab_orders(id) ON DELETE CASCADE,
    test_code VARCHAR(40) NOT NULL,         -- kod wewnętrzny lub LOINC
    test_name TEXT NOT NULL,                -- ENC (deterministic)
    unit VARCHAR(20),
    ref_range_low NUMERIC(14,4),
    ref_range_high NUMERIC(14,4),
    ref_range_text TEXT,
    result TEXT,                            -- ENC (random)
    result_numeric NUMERIC(14,4),           -- jeśli wynik jest liczbą – do wyliczenia abnormal flag
    abnormal_flag VARCHAR(2)                -- 'L','H','N','HH','LL'
        CHECK (abnormal_flag IS NULL OR abnormal_flag IN ('L','H','N','HH','LL')),
    status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending','completed','cancelled')),
    completed_at TIMESTAMPTZ,
    completed_by UUID REFERENCES emr.users(id),
    notes TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_lab_order_items_order ON emr.lab_order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_lab_order_items_status ON emr.lab_order_items(status);
CREATE INDEX IF NOT EXISTS idx_lab_order_items_test_code ON emr.lab_order_items(test_code);

-- ====== KATALOG PANELI ======
CREATE TABLE IF NOT EXISTS emr.lab_panels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(40) UNIQUE NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS emr.lab_panel_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    panel_id UUID NOT NULL REFERENCES emr.lab_panels(id) ON DELETE CASCADE,
    test_code VARCHAR(40) NOT NULL,
    test_name VARCHAR(200) NOT NULL,
    unit VARCHAR(20),
    ref_range_low NUMERIC(14,4),
    ref_range_high NUMERIC(14,4),
    ref_range_text TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    UNIQUE (panel_id, test_code)
);

CREATE INDEX IF NOT EXISTS idx_lab_panel_items_panel ON emr.lab_panel_items(panel_id);

-- ====== RLS dla nowych tabel ======
-- Zlecenia i pozycje – odczyt zgodny z dostępem do pacjenta; zapis: doctor/admin/lab_tech (item).
ALTER TABLE emr.lab_orders ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS lab_orders_read ON emr.lab_orders;
CREATE POLICY lab_orders_read ON emr.lab_orders
    FOR SELECT USING (emr.can_access_patient(patient_id));
DROP POLICY IF EXISTS lab_orders_write ON emr.lab_orders;
CREATE POLICY lab_orders_write ON emr.lab_orders
    FOR ALL USING (emr.current_user_role() IN ('admin','doctor','lab_tech'));

ALTER TABLE emr.lab_order_items ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS lab_order_items_read ON emr.lab_order_items;
CREATE POLICY lab_order_items_read ON emr.lab_order_items
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM emr.lab_orders o
            WHERE o.id = order_id
              AND emr.can_access_patient(o.patient_id)
        )
    );
DROP POLICY IF EXISTS lab_order_items_write ON emr.lab_order_items;
CREATE POLICY lab_order_items_write ON emr.lab_order_items
    FOR ALL USING (emr.current_user_role() IN ('admin','doctor','lab_tech'));

-- Panele są publicznym katalogiem (bez RLS)

-- ====== SEED PANELI ======
DO $$
DECLARE
    panel_id UUID;
BEGIN
    -- Morfologia podstawowa
    INSERT INTO emr.lab_panels(code, name, description)
    VALUES ('MORF_BASIC', 'Morfologia podstawowa', 'Pełna morfologia krwi obwodowej – badanie podstawowe')
    ON CONFLICT (code) DO NOTHING
    RETURNING id INTO panel_id;
    IF panel_id IS NOT NULL THEN
        INSERT INTO emr.lab_panel_items(panel_id, test_code, test_name, unit, ref_range_low, ref_range_high, sort_order) VALUES
            (panel_id, 'WBC',  'Leukocyty (WBC)',          '10^3/uL', 4.0,  10.0, 1),
            (panel_id, 'RBC',  'Erytrocyty (RBC)',         '10^6/uL', 4.2,  5.4,  2),
            (panel_id, 'HGB',  'Hemoglobina (HGB)',        'g/dL',    12.0, 17.5, 3),
            (panel_id, 'HCT',  'Hematokryt (HCT)',         '%',       37.0, 50.0, 4),
            (panel_id, 'PLT',  'Płytki krwi (PLT)',        '10^3/uL', 150.0,400.0,5),
            (panel_id, 'MCV',  'Średnia objętość krwinki', 'fL',      80.0, 100.0,6),
            (panel_id, 'MCH',  'Średnia masa Hb',          'pg',      27.0, 33.0, 7),
            (panel_id, 'MCHC', 'Stężenie Hb w krwince',    'g/dL',    32.0, 36.0, 8);
    END IF;

    -- Lipidogram
    INSERT INTO emr.lab_panels(code, name, description)
    VALUES ('LIPIDS', 'Lipidogram', 'Profil lipidowy – cholesterol całkowity, LDL, HDL, trójglicerydy')
    ON CONFLICT (code) DO NOTHING
    RETURNING id INTO panel_id;
    IF panel_id IS NOT NULL THEN
        INSERT INTO emr.lab_panel_items(panel_id, test_code, test_name, unit, ref_range_low, ref_range_high, sort_order) VALUES
            (panel_id, 'CHOL_T', 'Cholesterol całkowity', 'mg/dL', NULL, 200.0, 1),
            (panel_id, 'LDL',    'Cholesterol LDL',       'mg/dL', NULL, 115.0, 2),
            (panel_id, 'HDL',    'Cholesterol HDL',       'mg/dL', 40.0, NULL,  3),
            (panel_id, 'TG',     'Trójglicerydy',         'mg/dL', NULL, 150.0, 4);
    END IF;

    -- Tarczyca
    INSERT INTO emr.lab_panels(code, name, description)
    VALUES ('THYROID', 'Panel tarczycowy', 'TSH, FT3, FT4 – diagnostyka funkcji tarczycy')
    ON CONFLICT (code) DO NOTHING
    RETURNING id INTO panel_id;
    IF panel_id IS NOT NULL THEN
        INSERT INTO emr.lab_panel_items(panel_id, test_code, test_name, unit, ref_range_low, ref_range_high, sort_order) VALUES
            (panel_id, 'TSH', 'TSH',                 'uIU/mL', 0.27, 4.20, 1),
            (panel_id, 'FT3', 'Wolna trójjodotyronina','pg/mL', 2.04, 4.40, 2),
            (panel_id, 'FT4', 'Wolna tyroksyna',     'ng/dL',  0.93, 1.71, 3);
    END IF;

    -- Próby wątrobowe
    INSERT INTO emr.lab_panels(code, name, description)
    VALUES ('LIVER', 'Próby wątrobowe', 'ALT, AST, ALP, bilirubina – funkcja wątroby')
    ON CONFLICT (code) DO NOTHING
    RETURNING id INTO panel_id;
    IF panel_id IS NOT NULL THEN
        INSERT INTO emr.lab_panel_items(panel_id, test_code, test_name, unit, ref_range_low, ref_range_high, sort_order) VALUES
            (panel_id, 'ALT',   'ALT (AlAT)',         'U/L',   NULL, 40.0,  1),
            (panel_id, 'AST',   'AST (AspAT)',        'U/L',   NULL, 40.0,  2),
            (panel_id, 'ALP',   'Fosfataza alkaliczna','U/L',  40.0, 130.0, 3),
            (panel_id, 'BIL_T', 'Bilirubina całkowita','mg/dL', NULL, 1.20, 4);
    END IF;

    -- Funkcja nerek
    INSERT INTO emr.lab_panels(code, name, description)
    VALUES ('KIDNEY', 'Funkcja nerek', 'Mocznik, kreatynina, eGFR')
    ON CONFLICT (code) DO NOTHING
    RETURNING id INTO panel_id;
    IF panel_id IS NOT NULL THEN
        INSERT INTO emr.lab_panel_items(panel_id, test_code, test_name, unit, ref_range_low, ref_range_high, sort_order) VALUES
            (panel_id, 'UREA', 'Mocznik',         'mg/dL',           17.0, 43.0,  1),
            (panel_id, 'CREA', 'Kreatynina',      'mg/dL',           0.70, 1.30,  2),
            (panel_id, 'EGFR', 'eGFR',            'mL/min/1.73m^2',  90.0, NULL,  3);
    END IF;

    -- Diabetologiczny
    INSERT INTO emr.lab_panels(code, name, description)
    VALUES ('DIABETES', 'Panel diabetologiczny', 'Glukoza i hemoglobina glikowana')
    ON CONFLICT (code) DO NOTHING
    RETURNING id INTO panel_id;
    IF panel_id IS NOT NULL THEN
        INSERT INTO emr.lab_panel_items(panel_id, test_code, test_name, unit, ref_range_low, ref_range_high, sort_order) VALUES
            (panel_id, 'GLU',   'Glukoza na czczo',          'mg/dL', 70.0, 99.0, 1),
            (panel_id, 'HBA1C', 'Hemoglobina glikowana A1c', '%',     NULL, 5.7,  2);
    END IF;

    -- Elektrolity
    INSERT INTO emr.lab_panels(code, name, description)
    VALUES ('ELECTROLYTES', 'Elektrolity', 'Sód, potas, chlorki, wapń')
    ON CONFLICT (code) DO NOTHING
    RETURNING id INTO panel_id;
    IF panel_id IS NOT NULL THEN
        INSERT INTO emr.lab_panel_items(panel_id, test_code, test_name, unit, ref_range_low, ref_range_high, sort_order) VALUES
            (panel_id, 'NA', 'Sód (Na)',     'mmol/L', 136.0, 145.0, 1),
            (panel_id, 'K',  'Potas (K)',    'mmol/L', 3.50,  5.10,  2),
            (panel_id, 'CL', 'Chlorki (Cl)', 'mmol/L', 98.0,  107.0, 3),
            (panel_id, 'CA', 'Wapń (Ca)',    'mg/dL',  8.6,   10.3,  4);
    END IF;

    -- Stan zapalny
    INSERT INTO emr.lab_panels(code, name, description)
    VALUES ('INFLAMMATION', 'Markery zapalne', 'CRP, OB, leukocytoza')
    ON CONFLICT (code) DO NOTHING
    RETURNING id INTO panel_id;
    IF panel_id IS NOT NULL THEN
        INSERT INTO emr.lab_panel_items(panel_id, test_code, test_name, unit, ref_range_low, ref_range_high, sort_order) VALUES
            (panel_id, 'CRP', 'Białko C-reaktywne (CRP)', 'mg/L', NULL, 5.0,  1),
            (panel_id, 'ESR', 'OB',                       'mm/h', NULL, 20.0, 2),
            (panel_id, 'WBC', 'Leukocyty (WBC)',          '10^3/uL', 4.0, 10.0, 3);
    END IF;
END $$;

-- KONIEC V23

