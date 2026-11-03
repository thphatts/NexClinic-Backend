-- OpenMRS-style shared person model. Existing domain IDs stay unchanged.
CREATE TABLE persons (
    id BIGSERIAL PRIMARY KEY,
    gender VARCHAR(1),
    birthdate DATE,
    birthdate_estimated BOOLEAN NOT NULL DEFAULT FALSE,
    dead BOOLEAN NOT NULL DEFAULT FALSE,
    death_date TIMESTAMP,
    deathdate_estimated BOOLEAN NOT NULL DEFAULT FALSE,
    date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_changed TIMESTAMP,
    voided BOOLEAN NOT NULL DEFAULT FALSE,
    void_reason VARCHAR(255),
    uuid UUID NOT NULL DEFAULT gen_random_uuid()
);
CREATE UNIQUE INDEX uk_persons_uuid ON persons(uuid);

CREATE TABLE person_names (
    id BIGSERIAL PRIMARY KEY,
    person_id BIGINT NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    given_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255),
    family_name VARCHAR(255),
    preferred BOOLEAN NOT NULL DEFAULT TRUE,
    voided BOOLEAN NOT NULL DEFAULT FALSE,
    date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_person_names_person ON person_names(person_id);

CREATE TABLE person_addresses (
    id BIGSERIAL PRIMARY KEY,
    person_id BIGINT NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    address_text VARCHAR(255) NOT NULL,
    preferred BOOLEAN NOT NULL DEFAULT TRUE,
    voided BOOLEAN NOT NULL DEFAULT FALSE,
    date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_person_addresses_person ON person_addresses(person_id);

CREATE TABLE person_attributes (
    id BIGSERIAL PRIMARY KEY,
    person_id BIGINT NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    attribute_type VARCHAR(40) NOT NULL,
    attribute_value VARCHAR(255) NOT NULL,
    date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_person_attribute_lookup ON person_attributes(attribute_type, attribute_value);
CREATE INDEX idx_person_attribute_person ON person_attributes(person_id);

ALTER TABLE users ADD COLUMN person_id BIGINT;
ALTER TABLE patients ADD COLUMN person_id BIGINT;
ALTER TABLE doctors ADD COLUMN person_id BIGINT;
ALTER TABLE patients ADD COLUMN patient_code VARCHAR(32);
ALTER TABLE patients ADD COLUMN blood_type VARCHAR(20);
ALTER TABLE patients ADD COLUMN medical_history_summary TEXT;

-- Existing accounts become people first; user-linked patient/doctor records reuse these IDs.
CREATE TEMP TABLE legacy_person_map(source_type VARCHAR(10), source_id VARCHAR(255), person_id BIGINT);
WITH new_people AS (
    SELECT u.id::text AS source_id, nextval(pg_get_serial_sequence('persons', 'id')) AS person_id,
           COALESCE(u.created_at, CURRENT_TIMESTAMP) AS date_created
    FROM users u ORDER BY u.id
), inserted AS (
    INSERT INTO persons(id, gender, birthdate, date_created)
    SELECT person_id, NULL, NULL, date_created FROM new_people RETURNING id
)
INSERT INTO legacy_person_map
SELECT 'USER', source_id, person_id FROM new_people;

UPDATE users u SET person_id = m.person_id
FROM legacy_person_map m WHERE m.source_type = 'USER' AND m.source_id = u.id::text;

INSERT INTO person_names(person_id, given_name, middle_name, family_name, preferred)
SELECT u.person_id,
       reverse(split_part(reverse(n.full_name), ' ', 1)),
       NULLIF(BTRIM(regexp_replace(n.full_name, '^\S+\s*|\s*\S+$', '', 'g')), ''),
       CASE WHEN POSITION(' ' IN n.full_name) > 0 THEN split_part(n.full_name, ' ', 1) END, TRUE
FROM users u CROSS JOIN LATERAL (SELECT REGEXP_REPLACE(
    COALESCE(NULLIF(BTRIM(u.name), ''), NULLIF(BTRIM(u.username), ''), u.email), '\s+', ' ', 'g') AS full_name) n
WHERE u.person_id IS NOT NULL;
INSERT INTO person_addresses(person_id, address_text, preferred)
SELECT u.person_id, BTRIM(u.address), TRUE FROM users u
WHERE u.person_id IS NOT NULL AND NULLIF(BTRIM(u.address), '') IS NOT NULL;
INSERT INTO person_attributes(person_id, attribute_type, attribute_value)
SELECT u.person_id, 'PHONE', regexp_replace(BTRIM(u.phone), '\D', '', 'g') FROM users u
WHERE u.person_id IS NOT NULL AND NULLIF(BTRIM(u.phone), '') IS NOT NULL;
INSERT INTO person_attributes(person_id, attribute_type, attribute_value)
SELECT u.person_id, 'EMAIL', LOWER(BTRIM(u.email)) FROM users u
WHERE u.person_id IS NOT NULL AND NULLIF(BTRIM(u.email), '') IS NOT NULL;
INSERT INTO person_attributes(person_id, attribute_type, attribute_value)
SELECT u.person_id, 'CITIZEN_ID', BTRIM(u.citizen_id) FROM users u
WHERE u.person_id IS NOT NULL AND NULLIF(BTRIM(u.citizen_id), '') IS NOT NULL;

-- Patient rows with an existing user link share that user's person. Other patients retain their own identity.
UPDATE patients p SET person_id = u.person_id FROM users u WHERE p.user_id = u.id;
UPDATE patients p SET person_id = matched.person_id
FROM (
    SELECT old_patient.id, MIN(u.person_id) AS person_id
    FROM patients old_patient JOIN users u
      ON regexp_replace(COALESCE(old_patient.phone, ''), '\D', '', 'g') = regexp_replace(COALESCE(u.phone, ''), '\D', '', 'g')
     AND LOWER(BTRIM(old_patient.full_name)) = LOWER(BTRIM(u.name))
    WHERE old_patient.person_id IS NULL AND NULLIF(BTRIM(old_patient.phone), '') IS NOT NULL
    GROUP BY old_patient.id HAVING COUNT(*) = 1
) matched WHERE p.id = matched.id;
WITH new_people AS (
    SELECT p.id AS source_id, p.gender, p.dob, COALESCE(p.created_at, CURRENT_TIMESTAMP) AS date_created,
           nextval(pg_get_serial_sequence('persons', 'id')) AS person_id
    FROM patients p WHERE p.person_id IS NULL ORDER BY p.id
), inserted AS (
    INSERT INTO persons(id, gender, birthdate, date_created)
    SELECT person_id,
           CASE UPPER(BTRIM(gender)) WHEN 'NAM' THEN 'M' WHEN 'MALE' THEN 'M' WHEN 'M' THEN 'M'
                WHEN 'NỮ' THEN 'F' WHEN 'NU' THEN 'F' WHEN 'FEMALE' THEN 'F' WHEN 'F' THEN 'F'
                WHEN 'OTHER' THEN 'O' WHEN 'O' THEN 'O' ELSE 'U' END,
           dob, date_created FROM new_people RETURNING id
)
INSERT INTO legacy_person_map
SELECT 'PATIENT', source_id::text, person_id FROM new_people;
UPDATE patients p SET person_id = m.person_id
FROM legacy_person_map m WHERE m.source_type = 'PATIENT' AND m.source_id = p.id::text;
UPDATE persons person SET
    birthdate = COALESCE(person.birthdate, patient.dob),
    gender = CASE WHEN person.gender IS NULL OR person.gender = 'U' THEN
        CASE UPPER(BTRIM(patient.gender)) WHEN 'NAM' THEN 'M' WHEN 'MALE' THEN 'M' WHEN 'M' THEN 'M'
             WHEN 'NỮ' THEN 'F' WHEN 'NU' THEN 'F' WHEN 'FEMALE' THEN 'F' WHEN 'F' THEN 'F'
             WHEN 'OTHER' THEN 'O' WHEN 'O' THEN 'O' ELSE COALESCE(person.gender, 'U') END
        ELSE person.gender END
FROM patients patient WHERE person.id = patient.person_id;

INSERT INTO person_names(person_id, given_name, middle_name, family_name, preferred)
SELECT p.person_id, reverse(split_part(reverse(n.full_name), ' ', 1)),
       NULLIF(BTRIM(regexp_replace(n.full_name, '^\S+\s*|\s*\S+$', '', 'g')), ''),
       CASE WHEN POSITION(' ' IN n.full_name) > 0 THEN split_part(n.full_name, ' ', 1) END, TRUE
FROM patients p CROSS JOIN LATERAL (SELECT REGEXP_REPLACE(COALESCE(NULLIF(BTRIM(p.full_name), ''), 'Unknown'), '\s+', ' ', 'g') AS full_name) n
WHERE NOT EXISTS (SELECT 1 FROM person_names n WHERE n.person_id = p.person_id);
INSERT INTO person_addresses(person_id, address_text, preferred)
SELECT p.person_id, BTRIM(p.address), TRUE FROM patients p
WHERE NULLIF(BTRIM(p.address), '') IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM person_addresses a WHERE a.person_id = p.person_id);
INSERT INTO person_attributes(person_id, attribute_type, attribute_value)
SELECT p.person_id, 'PHONE', regexp_replace(BTRIM(p.phone), '\D', '', 'g') FROM patients p
WHERE NULLIF(BTRIM(p.phone), '') IS NOT NULL AND NOT EXISTS
 (SELECT 1 FROM person_attributes a WHERE a.person_id=p.person_id AND a.attribute_type='PHONE');
INSERT INTO person_attributes(person_id, attribute_type, attribute_value)
SELECT p.person_id, 'EMAIL', LOWER(BTRIM(p.email)) FROM patients p
WHERE NULLIF(BTRIM(p.email), '') IS NOT NULL AND NOT EXISTS
 (SELECT 1 FROM person_attributes a WHERE a.person_id=p.person_id AND a.attribute_type='EMAIL');
INSERT INTO person_attributes(person_id, attribute_type, attribute_value)
SELECT p.person_id, 'CITIZEN_ID', BTRIM(p.citizen_id) FROM patients p
WHERE NULLIF(BTRIM(p.citizen_id), '') IS NOT NULL AND NOT EXISTS
 (SELECT 1 FROM person_attributes a WHERE a.person_id=p.person_id AND a.attribute_type='CITIZEN_ID');

-- Doctors linked to an account reuse its person; standalone doctors receive a person record.
UPDATE doctors d SET person_id = u.person_id FROM users u WHERE d.user_id = u.id;
UPDATE doctors d SET person_id = matched.person_id
FROM (
    SELECT old_doctor.id, MIN(u.person_id) AS person_id
    FROM doctors old_doctor JOIN users u
      ON regexp_replace(COALESCE(old_doctor.phone, ''), '\D', '', 'g') = regexp_replace(COALESCE(u.phone, ''), '\D', '', 'g')
     AND LOWER(BTRIM(old_doctor.email)) = LOWER(BTRIM(u.email))
    WHERE old_doctor.person_id IS NULL AND NULLIF(BTRIM(old_doctor.phone), '') IS NOT NULL
    GROUP BY old_doctor.id HAVING COUNT(*) = 1
) matched WHERE d.id = matched.id;
WITH new_people AS (
    SELECT d.id AS source_id, COALESCE(d.created_at, CURRENT_TIMESTAMP) AS date_created,
           nextval(pg_get_serial_sequence('persons', 'id')) AS person_id
    FROM doctors d WHERE d.person_id IS NULL ORDER BY d.id
), inserted AS (
    INSERT INTO persons(id, gender, date_created)
    SELECT person_id, 'U', date_created FROM new_people RETURNING id
)
INSERT INTO legacy_person_map
SELECT 'DOCTOR', source_id::text, person_id FROM new_people;
UPDATE doctors d SET person_id = m.person_id
FROM legacy_person_map m WHERE m.source_type = 'DOCTOR' AND m.source_id = d.id::text;

INSERT INTO person_names(person_id, given_name, middle_name, family_name, preferred)
SELECT d.person_id, reverse(split_part(reverse(n.full_name), ' ', 1)),
       NULLIF(BTRIM(regexp_replace(n.full_name, '^\S+\s*|\s*\S+$', '', 'g')), ''),
       CASE WHEN POSITION(' ' IN n.full_name) > 0 THEN split_part(n.full_name, ' ', 1) END, TRUE
FROM doctors d CROSS JOIN LATERAL (SELECT REGEXP_REPLACE(COALESCE(NULLIF(BTRIM(d.full_name), ''), 'Unknown'), '\s+', ' ', 'g') AS full_name) n
WHERE NOT EXISTS (SELECT 1 FROM person_names n WHERE n.person_id=d.person_id);
INSERT INTO person_addresses(person_id, address_text, preferred)
SELECT d.person_id, BTRIM(u.address), TRUE FROM doctors d JOIN users u ON u.id=d.user_id
WHERE NULLIF(BTRIM(u.address), '') IS NOT NULL AND NOT EXISTS
 (SELECT 1 FROM person_addresses a WHERE a.person_id=d.person_id);
INSERT INTO person_attributes(person_id, attribute_type, attribute_value)
SELECT d.person_id, 'PHONE', regexp_replace(BTRIM(d.phone), '\D', '', 'g') FROM doctors d WHERE NULLIF(BTRIM(d.phone), '') IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM person_attributes a WHERE a.person_id=d.person_id AND a.attribute_type='PHONE');
INSERT INTO person_attributes(person_id, attribute_type, attribute_value)
SELECT d.person_id, 'EMAIL', LOWER(BTRIM(d.email)) FROM doctors d WHERE NULLIF(BTRIM(d.email), '') IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM person_attributes a WHERE a.person_id=d.person_id AND a.attribute_type='EMAIL');

UPDATE patients SET patient_code = 'PT-' || LPAD(id::text, 8, '0') WHERE patient_code IS NULL;
ALTER TABLE patients ALTER COLUMN patient_code SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_person FOREIGN KEY (person_id) REFERENCES persons(id);
ALTER TABLE users ALTER COLUMN person_id SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uk_users_person UNIQUE(person_id);
ALTER TABLE patients ADD CONSTRAINT fk_patients_person FOREIGN KEY (person_id) REFERENCES persons(id);
ALTER TABLE patients ALTER COLUMN person_id SET NOT NULL;
ALTER TABLE patients ADD CONSTRAINT uk_patients_person UNIQUE(person_id);
ALTER TABLE doctors ADD CONSTRAINT fk_doctors_person FOREIGN KEY (person_id) REFERENCES persons(id);
ALTER TABLE doctors ALTER COLUMN person_id SET NOT NULL;
ALTER TABLE doctors ADD CONSTRAINT uk_doctors_person UNIQUE(person_id);

-- V24 called this patient reference person_id; OpenMRS Obs.person_id identifies persons.
ALTER TABLE obs DROP CONSTRAINT IF EXISTS fk_obs_person;
UPDATE obs o SET person_id = p.person_id FROM patients p WHERE o.person_id = p.id;
ALTER TABLE obs ADD CONSTRAINT fk_obs_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE;

CREATE INDEX idx_patients_person_id ON patients(person_id);
CREATE INDEX idx_doctors_person_id ON doctors(person_id);
CREATE INDEX idx_users_person_id ON users(person_id);

-- Personal data now lives only on Person. Keep clinical and account identifiers unchanged.
ALTER TABLE patients DROP COLUMN full_name, DROP COLUMN citizen_id, DROP COLUMN phone,
    DROP COLUMN email, DROP COLUMN dob, DROP COLUMN gender, DROP COLUMN address, DROP COLUMN user_id;
ALTER TABLE users DROP COLUMN name, DROP COLUMN address, DROP COLUMN phone, DROP COLUMN citizen_id;
ALTER TABLE doctors DROP COLUMN full_name, DROP COLUMN phone, DROP COLUMN email;
