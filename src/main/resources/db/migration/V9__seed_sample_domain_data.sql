-- V9 (REVISED): Przykładowe dane + generator DEV/DEMO
-- UWAGA: Tylko dla środowisk deweloperskich. NIE UŻYWAĆ NA PRODUKCJI.
-- Pola wrażliwe (imię, nazwisko, pesel, adres, itp.) są w plaintext – po starcie
-- ustaw security.encryption.migrate=true aby zaszyfrować.
-- Jeśli ta migracja była już zastosowana, a chcesz nową wersję: potrzebny CLEAN bazy (DEV) albo flyway repair.

-- ====== UŻYTKOWNICY RDZENI ======
INSERT INTO emr.users (id, username, email, password_hash, role, password_algo)
VALUES
 ('0f3d0f9b-5f1d-4d9b-9fd7-111111111111','admin','admin@example.com',
  '$2a$12$1o1mjVhtAKYJH8tG6HC8J.D8jDPdTx66w30hgVZu7LOU/ZTrUytbW','admin','bcrypt')
ON CONFLICT DO NOTHING;

-- Dodatkowi użytkownicy kliniczni (stronger UUID randomness)
INSERT INTO emr.users (id, username, email, password_hash, role, password_algo)
VALUES
 ('91d9c6c9-a3fd-4d4d-8a0c-9a4c6c5d01d1','doctor_alfa','doctor.alfa@example.com','$2a$12$yW0y4cxr..VXNi2fL/95SO2S/bRMyCV2wAPOQgpdnH3UX0YPDhmCa','doctor','bcrypt'),
 ('b2fca7c4-8e3e-4798-8c06-1fe0dc4dd021','doctor_beta','doctor.beta@example.com','$2a$12$yW0y4cxr..VXNi2fL/95SO2S/bRMyCV2wAPOQgpdnH3UX0YPDhmCa','doctor','bcrypt'),
 ('e3b9b865-4ff9-4b70-a8c2-b1b0b911c3fd','nurse_anna','nurse.anna@example.com','$2a$12$yW0y4cxr..VXNi2fL/95SO2S/bRMyCV2wAPOQgpdnH3UX0YPDhmCa','nurse','bcrypt'),
 ('5b8e2a57-f0d4-4b86-9b56-0b6ec889a222','nurse_ola','nurse.ola@example.com','$2a$12$yW0y4cxr..VXNi2fL/95SO2S/bRMyCV2wAPOQgpdnH3UX0YPDhmCa','nurse','bcrypt'),
 ('c6a9bc3c-72d5-45b0-97d0-5a7a0a22d998','lab_marek','lab.marek@example.com','$2a$12$yW0y4cxr..VXNi2fL/95SO2S/bRMyCV2wAPOQgpdnH3UX0YPDhmCa','lab_tech','bcrypt')
ON CONFLICT DO NOTHING;

-- ====== PACJENCI (Kilka statycznych “referencyjnych”) ======
INSERT INTO emr.patients (id, first_name, last_name, date_of_birth, gender, pesel, contact_info, address, created_by, created_at)
VALUES
 ('11111111-aaaa-4444-8888-aaaaaaaaaaa1','Jan','Kowalski','1985-04-12','male','85041212345','{"phone":"+48111222333","email":"jan.kowalski@example.com"}','Ul. Kwiatowa 1, Warszawa','91d9c6c9-a3fd-4d4d-8a0c-9a4c6c5d01d1',now()),
 ('22222222-bbbb-4444-9999-bbbbbbbbbbb2','Anna','Nowak','1990-09-21','female','90092167890','{"phone":"+48999888777"}','Ul. Leśna 5, Kraków','91d9c6c9-a3fd-4d4d-8a0c-9a4c6c5d01d1',now()),
 ('33333333-cccc-4444-aaaa-ccccccccccc3','Piotr','Zieliński','1978-12-02','male','78120234567','{"phone":"+48555111222"}','Ul. Polna 10, Gdańsk','b2fca7c4-8e3e-4798-8c06-1fe0dc4dd021',now())
ON CONFLICT DO NOTHING;

-- ====== WSTĘPNE POWIĄZANIA DLA PACJENTÓW REFERENCYJNYCH ======
INSERT INTO emr.chronic_diseases (id, patient_id, disease_name, diagnosed_date, notes) VALUES
 (gen_random_uuid(),'11111111-aaaa-4444-8888-aaaaaaaaaaa1','Nadciśnienie','2015-06-01','Kontrola co 6 m-cy'),
 (gen_random_uuid(),'22222222-bbbb-4444-9999-bbbbbbbbbbb2','Cukrzyca typu 2','2019-01-10','Dieta + metformina')
ON CONFLICT DO NOTHING;

INSERT INTO emr.allergies (id, patient_id, allergen, reaction, severity, noted_by) VALUES
 (gen_random_uuid(),'11111111-aaaa-4444-8888-aaaaaaaaaaa1','Orzechy','Wysypka','moderate','91d9c6c9-a3fd-4d4d-8a0c-9a4c6c5d01d1'),
 (gen_random_uuid(),'22222222-bbbb-4444-9999-bbbbbbbbbbb2','Penicylina','Obrzęk','high','91d9c6c9-a3fd-4d4d-8a0c-9a4c6c5d01d1')
ON CONFLICT DO NOTHING;

INSERT INTO emr.medication_history (id, patient_id, medication, start_date, end_date, reason) VALUES
 (gen_random_uuid(),'11111111-aaaa-4444-8888-aaaaaaaaaaa1','Ramipril','2022-01-01',NULL,'Nadciśnienie'),
 (gen_random_uuid(),'22222222-bbbb-4444-9999-bbbbbbbbbbb2','Metformina','2023-05-10',NULL,'Kontrola glikemii')
ON CONFLICT DO NOTHING;

INSERT INTO emr.prescriptions (id, patient_id, doctor_id, medication, dosage_info, issued_date, expiration_date, is_repeatable) VALUES
 (gen_random_uuid(),'11111111-aaaa-4444-8888-aaaaaaaaaaa1','91d9c6c9-a3fd-4d4d-8a0c-9a4c6c5d01d1','Ramipril','1x5mg dziennie','2025-08-01','2025-12-31', true),
 (gen_random_uuid(),'22222222-bbbb-4444-9999-bbbbbbbbbbb2','b2fca7c4-8e3e-4798-8c06-1fe0dc4dd021','Metformina','2x500mg','2025-07-20','2025-11-30', true)
ON CONFLICT DO NOTHING;

INSERT INTO emr.visits (id, patient_id, doctor_id, visit_date, visit_type, reason, diagnosis, notes, is_confidential) VALUES
 (gen_random_uuid(),'11111111-aaaa-4444-8888-aaaaaaaaaaa1','91d9c6c9-a3fd-4d4d-8a0c-9a4c6c5d01d1', now() - interval '10 days','control','Kontrola nadciśnienia','Dobre wartości','Kontynuować leczenie', false),
 (gen_random_uuid(),'22222222-bbbb-4444-9999-bbbbbbbbbbb2','b2fca7c4-8e3e-4798-8c06-1fe0dc4dd021', now() - interval '5 days','consult','Dyskomfort','Podejrzenie nietolerancji laktozy','Test eliminacyjny', false)
ON CONFLICT DO NOTHING;

INSERT INTO emr.lab_results (id, patient_id, ordered_by, test_name, result, result_date, unit, reference_range, status) VALUES
 (gen_random_uuid(),'11111111-aaaa-4444-8888-aaaaaaaaaaa1','91d9c6c9-a3fd-4d4d-8a0c-9a4c6c5d01d1','Morfologia','W normie','2025-07-28',NULL,NULL,'completed'),
 (gen_random_uuid(),'22222222-bbbb-4444-9999-bbbbbbbbbbb2','b2fca7c4-8e3e-4798-8c06-1fe0dc4dd021','Glukoza na czczo','105','2025-07-30','mg/dL','70-99','completed')
ON CONFLICT DO NOTHING;

INSERT INTO emr.patient_consents (id, patient_id, granted_to_user_id, scope, granted_at, reason) VALUES
 (gen_random_uuid(),'11111111-aaaa-4444-8888-aaaaaaaaaaa1','e3b9b865-4ff9-4b70-a8c2-b1b0b911c3fd','read', now(),'Dostęp pielęgniarki'),
 (gen_random_uuid(),'22222222-bbbb-4444-9999-bbbbbbbbbbb2','5b8e2a57-f0d4-4b86-9b56-0b6ec889a222','read', now(),'Dostęp pielęgniarki')
ON CONFLICT DO NOTHING;

-- ====== HURTOWY GENERATOR DANYCH (zastępuje wcześniejsze V10) ======
-- Parametry generatora
DO $$
DECLARE
    target_count int := 400; -- docelowa łączna liczba pacjentów (zwiększ jeśli potrzebujesz więcej)
    existing     int;
    i            int;
    base_doctor  uuid;
    p_id         uuid;
    fnames       text[] := ARRAY['Jan','Anna','Piotr','Kasia','Marek','Ewa','Tomasz','Agnieszka','Paweł','Magda','Krzysztof','Joanna','Adam','Monika','Łukasz','Natalia','Michał','Barbara','Rafał','Justyna','Damian','Ola','Weronika','Patrycja','Karolina','Igor','Szymon','Oskar','Marta','Alicja','Dominik','Konrad','Hubert','Emilia','Zuzanna','Julia','Helena','Nadia','Oliwia','Gabriela'];
    lnames       text[] := ARRAY['Kowalski','Nowak','Wiśniewski','Dąbrowski','Lewandowski','Zieliński','Szymański','Wójcik','Kamiński','Kowalczyk','Piotrowski','Grabowski','Nowicki','Pawłowski','Król','Wieczorek','Majewski','Jabłoński','Gajewski','Michalski','Czarnecki','Sobczak','Urbański'];
    streets      text[] := ARRAY['Kwiatowa','Leśna','Polna','Szkolna','Ogrodowa','Lipowa','Jesionowa','Słoneczna','Krótka','Kościelna','Sportowa','Łąkowa','Brzozowa','Cicha','Miła','Długa','Akacjowa','Topolowa','Sosnowa','Żurawia'];
    cities       text[] := ARRAY['Warszawa','Kraków','Gdańsk','Poznań','Wrocław','Łódź','Szczecin','Lublin','Katowice','Bydgoszcz','Białystok','Rzeszów','Toruń'];
    genders      text[] := ARRAY['male','female'];
    diseases     text[] := ARRAY['Nadciśnienie','Cukrzyca typu 2','Astma','Choroba niedokrwienna serca','POChP','Otyłość','Migrena','Refluks','Depresja','Hashimoto','Anemia'];
    allergies    text[] := ARRAY['Penicylina','Orzechy','Pyłki','Kurz','Lateks','Jaja','Mleko','Sierść kota','Truskawki'];
    meds         text[] := ARRAY['Ramipril','Metformina','Salbutamol','Atorwastatyna','Omeprazol','Ibuprofen','Losartan','Amlodypina','Paracetamol','Enalapryl','Bisoprolol'];
    tests        text[] := ARRAY['Morfologia','Glukoza na czczo','Lipidogram','TSH','CRP','ALT','AST','HbA1c','Kreatynina','Bilirubina','D-dimer'];
    note_snips   text[] := ARRAY['Stabilny stan','Zalecana kontrola','Poprawa parametrów','Brak działań niepożądanych','Skargi na bóle głowy','Zalecenie zwiększenia aktywności fizycznej','Kontynuować dawkowanie','Rozważyć zmianę leku'];
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
    visit_iter   int;
    visit_cnt    int;
    snip         text;
BEGIN
    SELECT count(*) INTO existing FROM emr.patients;

    SELECT id INTO base_doctor FROM emr.users WHERE role='doctor' ORDER BY created_at LIMIT 1;
    IF base_doctor IS NULL THEN
        INSERT INTO emr.users(id, username, email, password_hash, role, password_algo)
        VALUES (gen_random_uuid(), 'dev_doctor_seed', 'dev_doctor_seed@example.com', '$2a$12$yW0y4cxr..VXNi2fL/95SO2S/bRMyCV2wAPOQgpdnH3UX0YPDhmCa', 'doctor','bcrypt')
        RETURNING id INTO base_doctor;
    END IF;

    IF existing >= target_count THEN
        RAISE NOTICE 'V9 generator: pomijam (pacjentów % >= target %)', existing, target_count;
        RETURN;
    END IF;

    FOR i IN existing+1 .. target_count LOOP
        first_name := fnames[(random()* (array_length(fnames,1)-1))::int + 1];
        last_name  := lnames[(random()* (array_length(lnames,1)-1))::int + 1];
        gender     := genders[(random()* (array_length(genders,1)-1))::int + 1];
        dob        := date '1940-01-01' + (random() * (date '2018-12-31' - date '1940-01-01'))::int;

        pesel_candidate := to_char(dob,'YYMMDD') || lpad((floor(random()*100000))::int::text,5,'0');
        WHILE EXISTS (SELECT 1 FROM emr.patients WHERE pesel = pesel_candidate) LOOP
            pesel_candidate := to_char(dob,'YYMMDD') || lpad((floor(random()*100000))::int::text,5,'0');
        END LOOP;

        addr := 'Ul. ' || streets[(random()* (array_length(streets,1)-1))::int + 1] || ' ' || (1 + (random()*140)::int)
                || ', ' || cities[(random()* (array_length(cities,1)-1))::int + 1];

        p_id := gen_random_uuid();
        INSERT INTO emr.patients(id, first_name, last_name, date_of_birth, gender, pesel, contact_info, address, created_by, created_at)
        VALUES (p_id, first_name, last_name, dob, gender, pesel_candidate,
                jsonb_build_object('phone', '+48'|| lpad((100000000 + (random()*899999999)::int)::text,9,'0')),
                addr, base_doctor, now());

        -- Choroby przewlekłe (0-2)
        FOR disease_name IN SELECT unnest(diseases) ORDER BY random() LIMIT (random()*2)::int LOOP
            INSERT INTO emr.chronic_diseases(id, patient_id, disease_name, diagnosed_date, notes)
            VALUES (gen_random_uuid(), p_id, disease_name, dob + (random()*4000)::int, 'Historia: '|| disease_name);
        END LOOP;

        -- Alergie (0-2)
        FOR allergy_name IN SELECT unnest(allergies) ORDER BY random() LIMIT (random()*2)::int LOOP
            INSERT INTO emr.allergies(id, patient_id, allergen, reaction, severity, noted_by)
            VALUES (gen_random_uuid(), p_id, allergy_name, 'Reakcja', (ARRAY['low','moderate','high'])[(random()*2)::int + 1], base_doctor);
        END LOOP;

        -- Historia leków (0-2)
        FOR medication IN SELECT unnest(meds) ORDER BY random() LIMIT (random()*2)::int LOOP
            INSERT INTO emr.medication_history(id, patient_id, medication, start_date, end_date, reason)
            VALUES (gen_random_uuid(), p_id, medication, dob + (random()*15000)::int, NULL, 'Terapia: '|| medication);
        END LOOP;

        -- Wizyty (1-3)
        visit_cnt := 1 + (random()*2)::int;
        FOR visit_iter IN 1..visit_cnt LOOP
            snip := note_snips[(random()* (array_length(note_snips,1)-1))::int + 1];
            INSERT INTO emr.visits(id, patient_id, doctor_id, visit_date, visit_type, reason, diagnosis, notes, is_confidential)
            VALUES (gen_random_uuid(), p_id, base_doctor, now() - (random()*540)::int * interval '1 day',
                    (ARRAY['control','consult','followup'])[(random()*2)::int + 1], 'Powód wizyty', 'Diagnoza', snip, false);
        END LOOP;

        -- Wyniki lab (0-3)
        FOR test_name IN SELECT unnest(tests) ORDER BY random() LIMIT (random()*3)::int LOOP
            INSERT INTO emr.lab_results(id, patient_id, ordered_by, test_name, result, result_date, unit, reference_range, status)
            VALUES (gen_random_uuid(), p_id, base_doctor, test_name, 'W normie', now()::date - (random()*300)::int, NULL, NULL, 'completed');
        END LOOP;

        -- Recepty (0-2)
        FOR medication IN SELECT unnest(meds) ORDER BY random() LIMIT (random()*2)::int LOOP
            INSERT INTO emr.prescriptions(id, patient_id, doctor_id, medication, dosage_info, issued_date, expiration_date, is_repeatable)
            VALUES (gen_random_uuid(), p_id, base_doctor, medication, 'Dawkowanie losowe', now()::date - (random()*90)::int, now()::date + (30 + (random()*180)::int), (random()<0.45));
        END LOOP;
    END LOOP;
END $$;

-- KONIEC V9 (REVISED)
