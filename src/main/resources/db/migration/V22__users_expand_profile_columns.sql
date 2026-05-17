-- V22: Rozszerzenie tabeli users o dane profilowe personelu medycznego
-- first_name, last_name – szyfrowane po stronie aplikacji (EncryptionEntityListener)
-- license_number – numer PWZ (prawo wykonywania zawodu) – dotyczy lekarzy/pielęgniarek
-- specialization – specjalizacja medyczna

ALTER TABLE emr.users
    ADD COLUMN IF NOT EXISTS first_name  varchar(200),
    ADD COLUMN IF NOT EXISTS last_name   varchar(200),
    ADD COLUMN IF NOT EXISTS license_number varchar(20),
    ADD COLUMN IF NOT EXISTS specialization varchar(100);

-- Indeks na nr PWZ (unikalny wśród niepustych)
CREATE UNIQUE INDEX IF NOT EXISTS users_license_number_uq
    ON emr.users (license_number)
    WHERE license_number IS NOT NULL;

-- Indeks przyspieszający wyszukiwanie po specjalizacji
CREATE INDEX IF NOT EXISTS users_specialization_idx
    ON emr.users (specialization)
    WHERE specialization IS NOT NULL;

