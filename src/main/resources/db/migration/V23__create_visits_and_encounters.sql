CREATE TABLE visits (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_visits_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT chk_visits_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);

CREATE TABLE encounters (
    id BIGSERIAL PRIMARY KEY,
    visit_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    encounter_type VARCHAR(30) NOT NULL,
    encounter_datetime TIMESTAMP NOT NULL,
    CONSTRAINT fk_encounters_visit
        FOREIGN KEY (visit_id) REFERENCES visits(id) ON DELETE CASCADE,
    CONSTRAINT fk_encounters_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_encounters_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE RESTRICT,
    CONSTRAINT chk_encounters_type
        CHECK (encounter_type IN ('VITALS', 'CONSULTATION', 'DISCHARGE'))
);

CREATE INDEX idx_visits_patient_start_time ON visits(patient_id, start_time DESC);
CREATE INDEX idx_encounters_visit_datetime ON encounters(visit_id, encounter_datetime);
CREATE INDEX idx_encounters_patient_datetime ON encounters(patient_id, encounter_datetime DESC);
CREATE INDEX idx_encounters_doctor_datetime ON encounters(doctor_id, encounter_datetime DESC);

-- Preserve existing records and create one closed visit and consultation encounter per record.
-- The legacy table remains available to the current medical-record and prescription flows.
INSERT INTO visits (id, patient_id, start_time, end_time, status)
SELECT id,
       patient_id,
       COALESCE(created_at, CURRENT_TIMESTAMP),
       COALESCE(updated_at, created_at, CURRENT_TIMESTAMP),
       'CLOSED'
FROM medical_records;

SELECT setval(
    pg_get_serial_sequence('visits', 'id'),
    COALESCE((SELECT MAX(id) FROM visits), 1),
    EXISTS (SELECT 1 FROM visits)
);

INSERT INTO encounters (visit_id, patient_id, doctor_id, encounter_type, encounter_datetime)
SELECT id,
       patient_id,
       doctor_id,
       'CONSULTATION',
       COALESCE(created_at, CURRENT_TIMESTAMP)
FROM medical_records;
