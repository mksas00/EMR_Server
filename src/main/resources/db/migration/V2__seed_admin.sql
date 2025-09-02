-- Seed konta administratora (hasło: Admin!123 - zmień po starcie)
-- Hash wygenerowany BCrypt cost 12
INSERT INTO emr.users (id, username, email, password_hash, role, created_at, password_algo)
VALUES (gen_random_uuid(), 'admin', 'admin@example.com', '$2a$12$kJVbAy7qjGSqWutZ6y/OjeigMgnbHxhjdBSPCuR6S8oX/8Pu2OPeK', 'admin', now(), 'bcrypt')
ON CONFLICT (username) DO NOTHING;

