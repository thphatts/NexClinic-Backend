-- Migration V9: Create medical_records, prescriptions, and prescription_items tables

-- 1. Create medical_records table
CREATE TABLE medical_records (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT UNIQUE NOT NULL,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    diagnosis TEXT NOT NULL,
    symptoms TEXT,
    notes TEXT,
    reexamination_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_medical_records_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    CONSTRAINT fk_medical_records_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_medical_records_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- 2. Create prescriptions table
CREATE TABLE prescriptions (
    id BIGSERIAL PRIMARY KEY,
    medical_record_id BIGINT UNIQUE NOT NULL,
    notes TEXT,
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_prescriptions_medical_record FOREIGN KEY (medical_record_id) REFERENCES medical_records(id) ON DELETE CASCADE
);

-- 3. Create prescription_items table
CREATE TABLE prescription_items (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    dosage VARCHAR(255) NOT NULL,
    unit_price NUMERIC(15, 2) NOT NULL,
    total_price NUMERIC(15, 2) NOT NULL,

    CONSTRAINT fk_prescription_items_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE,
    CONSTRAINT fk_prescription_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

-- Seed indexes for fast query lookup
CREATE INDEX idx_medical_records_patient_id ON medical_records(patient_id);
CREATE INDEX idx_medical_records_doctor_id ON medical_records(doctor_id);
CREATE INDEX idx_prescription_items_prescription_id ON prescription_items(prescription_id);
