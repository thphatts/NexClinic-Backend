ALTER TABLE visits ADD COLUMN appointment_id BIGINT UNIQUE;
ALTER TABLE visits ADD COLUMN visit_type VARCHAR(30) NOT NULL DEFAULT 'OUTPATIENT';
ALTER TABLE visits DROP CONSTRAINT IF EXISTS chk_visits_status;
UPDATE visits SET status = 'IN_PROGRESS' WHERE status = 'ACTIVE';
UPDATE visits SET status = 'COMPLETED' WHERE status = 'CLOSED';
ALTER TABLE visits ADD CONSTRAINT chk_visits_status
    CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'ACTIVE', 'CLOSED'));

ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_status_check;
ALTER TABLE appointments ADD CONSTRAINT appointments_status_check
    CHECK (status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED'));
