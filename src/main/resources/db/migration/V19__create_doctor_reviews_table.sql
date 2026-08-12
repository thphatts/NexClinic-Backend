CREATE TABLE IF NOT EXISTS doctor_reviews (
    id BIGSERIAL PRIMARY KEY,

    patient_id BIGINT REFERENCES patients(id) ON DELETE SET NULL,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    appointment_id BIGINT REFERENCES appointments(id) ON DELETE SET NULL,

    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    visit_count_snapshot INT,

    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_appointment_review
ON doctor_reviews (appointment_id)
WHERE appointment_id IS NOT NULL AND deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_doctor_reviews_doctor
ON doctor_reviews (doctor_id, created_at DESC)
WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_doctor_reviews_patient
ON doctor_reviews (patient_id);