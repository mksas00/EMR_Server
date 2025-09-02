-- V16: Seed minimal meds from legacy names and backfill medication_id, then drop text column
-- Assumes V15 added medication_id to emr.medication_history

-- 1) Create minimal medications from distinct names used in history and prescriptions
INSERT INTO emr.medications (id, name)
SELECT gen_random_uuid(), src.med_name
FROM (
  SELECT DISTINCT btrim(mh.medication) AS med_name
  FROM emr.medication_history mh
  WHERE mh.medication IS NOT NULL AND btrim(mh.medication) <> ''
  UNION
  SELECT DISTINCT btrim(p.medication) AS med_name
  FROM emr.prescriptions p
  WHERE p.medication IS NOT NULL AND btrim(p.medication) <> ''
) AS src
LEFT JOIN emr.medications m ON lower(m.name) = lower(src.med_name)
WHERE m.id IS NULL;

-- 2) Backfill medication_id in history by exact name match
UPDATE emr.medication_history mh
SET medication_id = m.id
FROM emr.medications m
WHERE mh.medication_id IS NULL
  AND lower(m.name) = lower(mh.medication);

-- 3) Finally drop legacy text column
ALTER TABLE emr.medication_history DROP COLUMN IF EXISTS medication;
