-- Migration V10: Enable PGVector extension and create clinic_knowledge_vectors table

-- 1. Enable vector extension if available in PostgreSQL
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS vector;
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'Extension vector could not be created directly or is not installed in PostgreSQL environment. Proceeding with standard fallback types.';
END $$;

-- 2. Create clinic_knowledge_vectors table
CREATE TABLE IF NOT EXISTS clinic_knowledge_vectors (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed initial knowledge items for RAG context
INSERT INTO clinic_knowledge_vectors (category, title, content)
VALUES
('CLINIC_INFO', 'Thông tin chung', 'Phòng khám AI-Powered Clinic Portal làm việc từ 07:30 - 20:00 hàng ngày kể cả Thứ 7 và Chủ Nhật. Địa chỉ: TP. Hồ Chí Minh. Hotline: 1900-1234.'),
('DOCTOR', 'Danh sách Bác sĩ', 'Bác sĩ Nguyễn Văn A - Chuyên khoa Hô Hấp (Kinh nghiệm 15 năm). Bác sĩ Trần Thị B - Chuyên khoa Tim Mạch (Kinh nghiệm 12 năm).'),
('SERVICE', 'Quy trình Đặt lịch', 'Khách hàng có thể đăng ký tài khoản online, tìm kiếm Bác sĩ theo Chuyên khoa và chọn khung giờ khám còn trống.'),
('PRODUCT', 'Danh mục Dược phẩm', 'Phòng khám cung cấp các loại thuốc đạt chuẩn GPP như Paracetamol 500mg, Amoxicillin 500mg, Berberin, Vitamin C.');
