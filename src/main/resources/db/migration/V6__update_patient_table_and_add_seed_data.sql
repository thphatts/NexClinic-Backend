-- Update patients table structure to match Patient Entity
ALTER TABLE patients ADD COLUMN IF NOT EXISTS phone VARCHAR(15) UNIQUE;
ALTER TABLE patients ADD COLUMN IF NOT EXISTS email VARCHAR(100);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS dob DATE;
ALTER TABLE patients ADD COLUMN IF NOT EXISTS gender VARCHAR(10);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS address VARCHAR(255);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE patients ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::TIMESTAMP;

-- Insert sample Categories if not exist
INSERT INTO category (id, name) VALUES
(1, 'Thuốc Kháng Sinh'),
(2, 'Thuốc Giảm Đau - Hạ Sốt'),
(3, 'Thuốc Tiêu Hóa'),
(4, 'Vật Tư Y Tế')
ON CONFLICT (id) DO NOTHING;

-- Reset Category ID sequence
SELECT setval('category_id_seq', (SELECT MAX(id) FROM category));

-- Insert sample Products if not exist
INSERT INTO products (id, name, price, status, category_id) VALUES
(1, 'Paracetamol 500mg', 15000.00, 'AVAILABLE', 2),
(2, 'Amoxicillin 500mg', 45000.00, 'AVAILABLE', 1),
(3, 'Berberin 50mg', 20000.00, 'AVAILABLE', 3),
(4, 'Khẩu Trang Y Tế 4 Lớp', 35000.00, 'AVAILABLE', 4),
(5, 'Efferalgan Codeine', 65000.00, 'AVAILABLE', 2)
ON CONFLICT (id) DO NOTHING;

-- Reset Products ID sequence
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));

-- Insert sample Patients
INSERT INTO patients (id, full_name, citizen_id, phone, email, dob, gender, address, created_at, updated_at) VALUES
(1, 'Nguyễn Văn An', '038099123456', '0987654321', 'nguyenvanan@gmail.com', '1990-05-15', 'NAM', '123 Nguyễn Trãi, Quận 1, TP. Hồ Chí Minh', NOW(), NOW()),
(2, 'Trần Thị Bích', '038099234567', '0912345678', 'tranthibich@gmail.com', '1995-08-20', 'NỮ', '456 Lê Lợi, Quận 3, TP. Hồ Chí Minh', NOW(), NOW()),
(3, 'Lê Hoàng Nam', '038099345678', '0903123456', 'lehoangnam@gmail.com', '1988-12-10', 'NAM', '789 Trần Hưng Đạo, Quận 5, TP. Hồ Chí Minh', NOW(), NOW()),
(4, 'Phạm Minh Thu', '038099456789', '0978901234', 'phamminhthu@gmail.com', '2001-03-25', 'NỮ', '12 Hoàng Diệu, Quận Bình Thạnh, TP. Hồ Chí Minh', NOW(), NOW()),
(5, 'Đỗ Quốc Anh', '038099567890', '0934567890', 'doquocanh@gmail.com', '1997-11-05', 'NAM', '99 Nguyễn Văn Linh, Quận 7, TP. Hồ Chí Minh', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Reset Patients ID sequence
SELECT setval('patients_id_seq', (SELECT MAX(id) FROM patients));

-- Insert sample Checkups (Lịch sử khám bệnh)
INSERT INTO checkups (id, diagnoses, patient_id) VALUES
(1, 'Cảm cúm thông thường, viêm họng nhẹ', 1),
(2, 'Rối loạn tiêu hóa cấp tính', 1),
(3, 'Viêm xoang mãn tính tái phát', 2),
(4, 'Đau dạ dày do trào ngược gastroesophageal', 3),
(5, 'Khám sức khỏe định kỳ - Chỉ số bình thường', 4),
(6, 'Viêm phế quản cấp tính, sốt 38.5 độ', 5)
ON CONFLICT (id) DO NOTHING;

-- Reset Checkups ID sequence
SELECT setval('checkups_id_seq', (SELECT MAX(id) FROM checkups));
