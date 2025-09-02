-- V8: Rozszerzenie długości kolumn pacjenta pod szyfrowanie (ciphertext dłuższy niż plaintext)
-- Uruchomić po wdrożeniu generycznego szyfrowania w encji Patient.

BEGIN;

-- first_name
ALTER TABLE emr.patients
    ALTER COLUMN first_name TYPE varchar(300);

-- last_name
ALTER TABLE emr.patients
    ALTER COLUMN last_name TYPE varchar(300);

-- pesel (był varchar(11), teraz ciphertext deterministyczny do 200)
ALTER TABLE emr.patients
    ALTER COLUMN pesel TYPE varchar(200);

COMMIT;

