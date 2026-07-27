-- Create doctors table
CREATE TABLE IF NOT EXISTS doctors (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    degree VARCHAR(100),
    phone VARCHAR(15) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    experience_years INT DEFAULT 0,
    consultation_fee NUMERIC(19, 2) DEFAULT 0.00,
    user_id VARCHAR(36) UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create appointments table
CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    appointment_date DATE NOT NULL,
    time_slot VARCHAR(50) NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    reason VARCHAR(255),
    notes VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed Doctors
INSERT INTO doctors (id, full_name, specialization, degree, phone, email, experience_years, consultation_fee, created_at, updated_at) VALUES
(1, 'Bác sĩ Nguyễn Văn A', 'Nội Khoa', 'Thạc sĩ - Bác sĩ CKI', '0900000002', 'doctor@clinic.com', 10, 200000.00, NOW(), NOW()),
(2, 'Bác sĩ Trần Thi B', 'Nhi Khoa', 'Tiến sĩ Y Học', '0900000005', 'doctor.tranb@clinic.com', 15, 250000.00, NOW(), NOW()),
(3, 'Bác sĩ Lê Hoàng C', 'Tai Mũi Họng', 'Bác sĩ CKII', '0900000006', 'doctor.lec@clinic.com', 8, 180000.00, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Reset Doctors ID sequence
SELECT setval('doctors_id_seq', (SELECT MAX(id) FROM doctors));

-- Seed Appointments
INSERT INTO appointments (id, patient_id, doctor_id, appointment_date, time_slot, status, reason, notes, created_at, updated_at) VALUES
(1, 1, 1, CURRENT_DATE + INTERVAL '1 day', '08:00 - 08:30', 'CONFIRMED', 'Tái khám định kỳ huyết áp', 'Bệnh nhân mang theo kết quả xét nghiệm cũ', NOW(), NOW()),
(2, 2, 2, CURRENT_DATE + INTERVAL '1 day', '09:00 - 09:30', 'PENDING', 'Khám ho và sốt nhẹ ở trẻ em', 'Trẻ 5 tuổi', NOW(), NOW()),
(3, 3, 3, CURRENT_DATE + INTERVAL '2 day', '10:00 - 10:30', 'CONFIRMED', 'Khám tai mũi họng do đau họng kéo dài', NULL, NOW(), NOW()),
(4, 4, 1, CURRENT_DATE + INTERVAL '2 day', '14:00 - 14:30', 'PENDING', 'Khám đau dạ dày', NULL, NOW(), NOW()),
(5, 5, 2, CURRENT_DATE + INTERVAL '3 day', '15:00 - 15:30', 'CANCELLED', 'Khám sức khỏe tổng quát', 'Bệnh nhân hủy lịch do bận công tác', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Reset Appointments ID sequence
SELECT setval('appointments_id_seq', (SELECT MAX(id) FROM appointments));
