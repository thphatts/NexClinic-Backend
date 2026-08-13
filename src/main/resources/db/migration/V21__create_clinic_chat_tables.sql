-- Migration V21: Create clinic chat tables for real-time doctor-patient messaging
-- Uses prefix 'clinic_' to avoid collision with 'ai_chat_messages' table (V11)

CREATE TABLE IF NOT EXISTS clinic_chat_rooms (
    id             BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT REFERENCES appointments(id) ON DELETE SET NULL,
    doctor_id      BIGINT NOT NULL,
    patient_id     BIGINT NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_clinic_chat_rooms_doctor   ON clinic_chat_rooms(doctor_id);
CREATE INDEX IF NOT EXISTS idx_clinic_chat_rooms_patient  ON clinic_chat_rooms(patient_id);
CREATE INDEX IF NOT EXISTS idx_clinic_chat_rooms_appt     ON clinic_chat_rooms(appointment_id);

CREATE TABLE IF NOT EXISTS clinic_chat_messages (
    id          BIGSERIAL PRIMARY KEY,
    room_id     BIGINT NOT NULL REFERENCES clinic_chat_rooms(id) ON DELETE CASCADE,
    sender_id   VARCHAR(255) NOT NULL,
    sender_name VARCHAR(255),
    sender_role VARCHAR(30) NOT NULL,
    content     TEXT NOT NULL,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    read_at     TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_clinic_chat_messages_room_id   ON clinic_chat_messages(room_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_clinic_chat_messages_sender    ON clinic_chat_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_clinic_chat_messages_unread    ON clinic_chat_messages(room_id, is_read) WHERE is_read = FALSE;
