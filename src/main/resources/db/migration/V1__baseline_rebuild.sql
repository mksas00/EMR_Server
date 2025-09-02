-- NOWA BAZOWA MIGRACJA (REBUILD) oparta na encjach + zabezpieczenia
-- Użyj TYLKO na czystej bazie (usuń flyway_schema_history, stare migracje i zdropuj DB).
-- PostgreSQL 16

CREATE SCHEMA IF NOT EXISTS emr;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

-- UŻYTKOWNICY
CREATE TABLE IF NOT EXISTS emr.users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    username varchar(50) NOT NULL,
    email varchar(100) NOT NULL,
    password_hash text NOT NULL,
    role varchar(20) NOT NULL,
    created_at timestamptz DEFAULT now(),
    last_password_change timestamptz,
    is_account_locked boolean DEFAULT false,
    failed_login_attempts int DEFAULT 0,
    mfa_secret varchar(64),
    last_login_at timestamptz,
    last_login_ip inet,
    password_algo varchar(30),
    CONSTRAINT users_username_uq UNIQUE (username),
    CONSTRAINT users_email_uq UNIQUE (email),
    CONSTRAINT users_role_chk CHECK (role IN ('admin','doctor','nurse','lab_tech','receptionist'))
);

-- PACJENCI
CREATE TABLE IF NOT EXISTS emr.patients (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name varchar(50) NOT NULL,
    last_name varchar(50) NOT NULL,
    date_of_birth date NOT NULL,
    gender varchar(10),
    pesel varchar(11) NOT NULL,
    contact_info jsonb,
    address text,
    created_by uuid REFERENCES emr.users(id) ON DELETE SET NULL,
    created_at timestamptz DEFAULT now(),
    CONSTRAINT patients_pesel_uq UNIQUE (pesel)
);

-- AUDYT
CREATE SEQUENCE IF NOT EXISTS emr.audit_log_id_seq START 1;
CREATE TABLE IF NOT EXISTS emr.audit_log (
    id integer PRIMARY KEY DEFAULT nextval('emr.audit_log_id_seq'),
    user_id uuid REFERENCES emr.users(id) ON DELETE SET NULL,
    patient_id uuid REFERENCES emr.patients(id) ON DELETE CASCADE,
    action varchar(50),
    description text,
    "timestamp" timestamptz DEFAULT now()
);

-- WERSJONOWANIE
CREATE TABLE IF NOT EXISTS emr.data_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type varchar(50),
    entity_id uuid,
    modified_by uuid REFERENCES emr.users(id) ON DELETE SET NULL,
    changed_by uuid REFERENCES emr.users(id) ON DELETE SET NULL,
    change_reason text,
    old_data jsonb,
    new_data jsonb,
    modified_at timestamptz DEFAULT now()
);
CREATE INDEX dv_entity_idx ON emr.data_versions(entity_type, entity_id);

-- ALLERGIES
CREATE TABLE IF NOT EXISTS emr.allergies (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id uuid NOT NULL REFERENCES emr.patients(id) ON DELETE CASCADE,
    allergen text NOT NULL,
    reaction text,
    severity varchar(20),
    noted_by uuid REFERENCES emr.users(id) ON DELETE SET NULL
);

-- CHRONIC DISEASES
CREATE TABLE IF NOT EXISTS emr.chronic_diseases (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id uuid NOT NULL REFERENCES emr.patients(id) ON DELETE CASCADE,
    disease_name text NOT NULL,
    diagnosed_date date,
    notes text
);

-- LAB RESULTS
CREATE TABLE IF NOT EXISTS emr.lab_results (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id uuid NOT NULL REFERENCES emr.patients(id) ON DELETE CASCADE,
    ordered_by uuid REFERENCES emr.users(id) ON DELETE SET NULL,
    test_name text NOT NULL,
    result text,
    result_date date,
    unit varchar(20),
    reference_range text,
    status varchar(20) DEFAULT 'completed'
);

-- MEDICAL FILES
CREATE TABLE IF NOT EXISTS emr.medical_files (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id uuid NOT NULL REFERENCES emr.patients(id) ON DELETE CASCADE,
    uploaded_by uuid REFERENCES emr.users(id) ON DELETE SET NULL,
    file_name text,
    file_path text,
    mime_type text,
    uploaded_at timestamptz DEFAULT now()
);
CREATE INDEX medical_files_patient_idx ON emr.medical_files(patient_id);

-- MEDICAL RECORDS
CREATE TABLE IF NOT EXISTS emr.medical_records (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id uuid NOT NULL REFERENCES emr.patients(id) ON DELETE CASCADE,
    created_by uuid REFERENCES emr.users(id) ON DELETE SET NULL,
    record_type varchar(50) NOT NULL,
    content jsonb,
    is_encrypted boolean DEFAULT false,
    encrypted_checksum varchar(128),
    created_at timestamptz DEFAULT now()
);
CREATE INDEX medical_records_patient_idx ON emr.medical_records(patient_id);
CREATE INDEX medical_records_type_idx ON emr.medical_records(record_type);

-- MEDICATION HISTORY
CREATE TABLE IF NOT EXISTS emr.medication_history (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id uuid NOT NULL REFERENCES emr.patients(id) ON DELETE CASCADE,
    medication text NOT NULL,
    start_date date,
    end_date date,
    reason text
);

-- PRESCRIPTIONS
CREATE TABLE IF NOT EXISTS emr.prescriptions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id uuid NOT NULL REFERENCES emr.patients(id) ON DELETE CASCADE,
    doctor_id uuid NOT NULL REFERENCES emr.users(id) ON DELETE SET NULL,
    medication text NOT NULL,
    dosage_info text,
    issued_date date,
    expiration_date date,
    is_repeatable boolean DEFAULT false
);
CREATE INDEX prescriptions_patient_idx ON emr.prescriptions(patient_id);

-- VISITS
CREATE TABLE IF NOT EXISTS emr.visits (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id uuid NOT NULL REFERENCES emr.patients(id) ON DELETE CASCADE,
    doctor_id uuid NOT NULL REFERENCES emr.users(id) ON DELETE SET NULL,
    visit_date timestamptz NOT NULL,
    end_date timestamptz,
    visit_type varchar(50),
    reason text,
    diagnosis text,
    notes text,
    is_confidential boolean DEFAULT false,
    status varchar(20) DEFAULT 'PLANNED'
);
CREATE INDEX visits_patient_idx ON emr.visits(patient_id);
CREATE INDEX visits_doctor_idx ON emr.visits(doctor_id);

-- PERMISSIONS
CREATE SEQUENCE emr.permissions_id_seq START 1;
CREATE TABLE IF NOT EXISTS emr.permissions (
    id integer PRIMARY KEY DEFAULT nextval('emr.permissions_id_seq'),
    role varchar(20) NOT NULL,
    resource varchar(50) NOT NULL,
    action varchar(20) NOT NULL,
    permission_type varchar(20),
    access_level varchar(20)
);
CREATE INDEX permissions_role_res_idx ON emr.permissions(role, resource);

-- SECURITY: PASSWORD HISTORY
CREATE TABLE IF NOT EXISTS emr.password_history (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES emr.users(id) ON DELETE CASCADE,
    password_hash text NOT NULL,
    password_algo varchar(30) NOT NULL,
    created_at timestamptz DEFAULT now()
);
CREATE INDEX password_history_user_created_idx ON emr.password_history(user_id, created_at DESC);

-- SECURITY: LOGIN ATTEMPTS
CREATE TABLE IF NOT EXISTS emr.user_login_attempts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid REFERENCES emr.users(id) ON DELETE SET NULL,
    ts timestamptz DEFAULT now(),
    success boolean NOT NULL,
    ip inet,
    user_agent varchar(300)
);
CREATE INDEX user_login_attempts_user_ts_idx ON emr.user_login_attempts(user_id, ts DESC);
CREATE INDEX user_login_attempts_failed_idx ON emr.user_login_attempts(user_id) WHERE success = false;

-- SECURITY: SESSIONS
CREATE TABLE IF NOT EXISTS emr.user_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES emr.users(id) ON DELETE CASCADE,
    issued_at timestamptz DEFAULT now(),
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    ip inet,
    user_agent varchar(300),
    refresh_token_hash text UNIQUE
);
CREATE INDEX user_sessions_user_idx ON emr.user_sessions(user_id, expires_at DESC);
CREATE INDEX user_sessions_active_idx ON emr.user_sessions(user_id) WHERE revoked_at IS NULL;
CREATE INDEX user_sessions_expires_idx ON emr.user_sessions(expires_at);

-- SECURITY: PATIENT CONSENTS
CREATE TABLE IF NOT EXISTS emr.patient_consents (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id uuid NOT NULL REFERENCES emr.patients(id) ON DELETE CASCADE,
    granted_to_user_id uuid REFERENCES emr.users(id) ON DELETE CASCADE,
    scope varchar(100) NOT NULL,
    granted_at timestamptz DEFAULT now(),
    revoked_at timestamptz,
    reason text
);
CREATE INDEX patient_consents_patient_idx ON emr.patient_consents(patient_id);
CREATE INDEX patient_consents_granted_to_idx ON emr.patient_consents(granted_to_user_id);
CREATE UNIQUE INDEX patient_consents_unique_active ON emr.patient_consents(patient_id, granted_to_user_id, scope) WHERE revoked_at IS NULL;

-- SECURITY: INCIDENTS
CREATE TABLE IF NOT EXISTS emr.security_incidents (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    detected_at timestamptz DEFAULT now(),
    severity varchar(20) NOT NULL,
    category varchar(50) NOT NULL,
    description text,
    user_id uuid REFERENCES emr.users(id) ON DELETE SET NULL,
    status varchar(20) DEFAULT 'open'
);
CREATE INDEX security_incidents_status_idx ON emr.security_incidents(status);
CREATE INDEX security_incidents_severity_idx ON emr.security_incidents(severity);

-- INDEKSY DODATKOWE
CREATE INDEX patients_created_by_idx ON emr.patients(created_by);
CREATE INDEX lab_results_patient_idx ON emr.lab_results(patient_id);
CREATE INDEX medical_files_uploaded_by_idx ON emr.medical_files(uploaded_by);
CREATE INDEX medical_records_created_by_idx ON emr.medical_records(created_by);

-- (Opcjonalnie) Możesz później dodać RLS: ALTER TABLE emr.medical_records ENABLE ROW LEVEL SECURITY; itd.
-- Koniec baseline
