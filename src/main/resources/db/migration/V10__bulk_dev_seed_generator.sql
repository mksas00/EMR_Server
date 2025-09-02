-- V10: Hurtowy seed danych DEV / DEMO (generator losowych pacjentów i rekordów medycznych)
-- UWAGA: Do użycia WYŁĄCZNIE w środowiskach deweloperskich / demonstracyjnych.
-- Pola wrażliwe zapisane w plaintext – po starcie aplikacji uruchom migrację szyfrowania
-- (security.encryption.migrate=true) aby zaszyfrować wartości.
-- Jeżeli w bazie jest już >= target_count pacjentów migracja generatora nic nie zrobi.

-- Parametry (zmień target_count jeśli chcesz więcej/mniej pacjentów):
DO $$
DECLARE
    target_count int := 200;          -- docelowa liczba pacjentów (łącznie, uwzględnia już istniejących)
    existing     int;
    i            int;
    base_doctor  uuid;
    p_id         uuid;
    fnames       text[] := ARRAY['Jan','Anna','Piotr','Kasia','Marek','Ewa','Tomasz','Agnieszka','Paweł','Magda','Krzysztof','Joanna','Adam','Monika','Łukasz','Natalia','Michał','Barbara','Rafał','Justyna'];
    lnames       text[] := ARRAY['Kowalski','Nowak','Wiśniewski','Dąbrowski','Lewandowski','Zieliński','Szymański','Wójcik','Kamiński','Kowalczyk','Piotrowski','Grabowski','Nowicki','Pawłowski'];
    streets      text[] := ARRAY['Kwiatowa','Leśna','Polna','Szkolna','Ogrodowa','Lipowa','Jesionowa','Słoneczna','Krótka','Kościelna','Sportowa','Łąkowa','Brzozowa','Cicha'];
    cities       text[] := ARRAY['Warszawa','Kraków','Gdańsk','Poznań','Wrocław','Łódź','Szczecin','Lublin','Katowice'];
    genders      text[] := ARRAY['male','female'];
    diseases     text[] := ARRAY['Nadciśnienie','Cukrzyca typu 2','Astma','Choroba niedokrwienna serca','POChP','Otyłość','Migrena','Refluks','Depresja'];
    allergies    text[] := ARRAY['Penicylina','Orzechy','Pyłki','Kurz','Lateks','Jaja','Mleko','Sierść kota'];
    meds         text[] := ARRAY['Ramipril','Metformina','Salbutamol','Atorwastatyna','Omeprazol','Ibuprofen','Losartan','Amlodypina'];
    tests        text[] := ARRAY['Morfologia','Glukoza na czczo','Lipidogram','TSH','CRP','ALT','AST','Hemoglobina glikowana','Kreatynina'];
    first_name   text;
    last_name    text;
    gender       text;
    dob          date;
    pesel_candidate text;
    addr         text;
    disease_name text;
    allergy_name text;
    medication   text;
    test_name    text;
    visit_count  int;
    ins_count    int := 0;
BEGIN
    SELECT count(*) INTO existing FROM emr.patients;

    -- Wybierz istniejącego lekarza; jeśli brak — utwórz technicznego.
    SELECT id INTO base_doctor FROM emr.users WHERE role='doctor' ORDER BY created_at LIMIT 1;
    IF base_doctor IS NULL THEN
        INSERT INTO emr.users(id, username, email, password_hash, role, password_algo)
        VALUES (gen_random_uuid(), 'dev_doctor', 'dev_doctor@example.com', '$2a$12$yW0y4cxr..VXNi2fL/95SO2S/bRMyCV2wAPOQgpdnH3UX0YPDhmCa', 'doctor','bcrypt')
        RETURNING id INTO base_doctor;
    END IF;

    IF existing >= target_count THEN
        RAISE NOTICE 'V10 generator: pomijam (pacjentów % >= target %)', existing, target_count;
        RETURN;
    END IF;

    FOR i IN existing+1 .. target_count LOOP
        first_name := fnames[(random()* (array_length(fnames,1)-1))::int + 1];
        last_name  := lnames[(random()* (array_length(lnames,1)-1))::int + 1];
        gender     := genders[(random()* (array_length(genders,1)-1))::int + 1];
        dob        := date '1940-01-01' + (random() * (date '2018-12-31' - date '1940-01-01'))::int;

        -- PESEL (pseudo): YYMMDD + losowe 5 cyfr, aż unikalny.
        pesel_candidate := to_char(dob,'YYMMDD') || lpad((floor(random()*100000))::int::text,5,'0');
        WHILE EXISTS (SELECT 1 FROM emr.patients WHERE pesel = pesel_candidate) LOOP
            pesel_candidate := to_char(dob,'YYMMDD') || lpad((floor(random()*100000))::int::text,5,'0');
        END LOOP;

        addr := 'Ul. ' || streets[(random()* (array_length(streets,1)-1))::int + 1] || ' ' || (1 + (random()*120)::int)
                || ', ' || cities[(random()* (array_length(cities,1)-1))::int + 1];

        p_id := gen_random_uuid();
        INSERT INTO emr.patients(id, first_name, last_name, date_of_birth, gender, pesel, contact_info, address, created_by, created_at)
        VALUES (p_id, first_name, last_name, dob, gender, pesel_candidate,
                jsonb_build_object('phone', '+48'|| lpad((100000000 + (random()*899999999)::int)::text,9,'0')),
                addr, base_doctor, now());

        ins_count := ins_count + 1;

        -- Choroby przewlekłe (0-2)
        FOR disease_name IN SELECT unnest(diseases) ORDER BY random() LIMIT (random()*2)::int LOOP
            INSERT INTO emr.chronic_diseases(id, patient_id, disease_name, diagnosed_date, notes)
            VALUES (gen_random_uuid(), p_id, disease_name, dob + (random()*5000)::int, 'Notatka choroba');
        END LOOP;

        -- Alergie (0-2)
        FOR allergy_name IN SELECT unnest(allergies) ORDER BY random() LIMIT (random()*2)::int LOOP
            INSERT INTO emr.allergies(id, patient_id, allergen, reaction, severity, noted_by)
            VALUES (gen_random_uuid(), p_id, allergy_name, 'Reakcja', (ARRAY['low','moderate','high'])[(random()*2)::int + 1], base_doctor);
        END LOOP;

        -- Historia leków (0-2)
        FOR medication IN SELECT unnest(meds) ORDER BY random() LIMIT (random()*2)::int LOOP
            INSERT INTO emr.medication_history(id, patient_id, medication, start_date, end_date, reason)
            VALUES (gen_random_uuid(), p_id, medication, dob + (random()*15000)::int, NULL, 'Terapia');
        END LOOP;

        -- Wizyty (1-3)
        visit_count := 1 + (random()*2)::int;
        FOR i IN 1..visit_count LOOP
            INSERT INTO emr.visits(id, patient_id, doctor_id, visit_date, visit_type, reason, diagnosis, notes, is_confidential)
            VALUES (gen_random_uuid(), p_id, base_doctor, now() - (random()*365)::int * interval '1 day',
                    (ARRAY['control','consult','followup'])[(random()*2)::int + 1], 'Powód wizyty', 'Diagnoza', 'Notatki', false);
        END LOOP;

        -- Wyniki lab (0-2)
        FOR test_name IN SELECT unnest(tests) ORDER BY random() LIMIT (random()*2)::int LOOP
            INSERT INTO emr.lab_results(id, patient_id, ordered_by, test_name, result, result_date, unit, reference_range, status)
            VALUES (gen_random_uuid(), p_id, base_doctor, test_name, 'W normie', now()::date - (random()*200)::int, NULL, NULL, 'completed');
        END LOOP;

        -- Recepty (0-2)
        FOR medication IN SELECT unnest(meds) ORDER BY random() LIMIT (random()*2)::int LOOP
            INSERT INTO emr.prescriptions(id, patient_id, doctor_id, medication, dosage_info, issued_date, expiration_date, is_repeatable)
            VALUES (gen_random_uuid(), p_id, base_doctor, medication, 'Dawkowanie', now()::date - (random()*60)::int, now()::date + (30 + (random()*120)::int), (random()<0.4));
        END LOOP;
    END LOOP;

    RAISE NOTICE 'V10 generator: dodano % nowych pacjentów (docelowo %).', ins_count, target_count;
END $$;

-- KONIEC V10

