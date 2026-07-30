-- Prevent double booking on active appointments (status != 'CANCELLED') for the same doctor, date and time slot
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_doctor_active_slot
ON appointments (doctor_id, appointment_date, time_slot)
WHERE status != 'CANCELLED';
