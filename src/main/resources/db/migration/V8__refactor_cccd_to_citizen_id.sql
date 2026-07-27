-- Migration V8: Refactor cccd to citizen_id in users table safely and add user_id link to patients table

-- 1. Safely rename cccd column if it exists, or add citizen_id if it doesn't exist yet
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'cccd'
    ) THEN
        ALTER TABLE users RENAME COLUMN cccd TO citizen_id;
    ELSIF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'citizen_id'
    ) THEN
        ALTER TABLE users ADD COLUMN citizen_id VARCHAR(12) UNIQUE;
    END IF;
END $$;

-- 2. Add user_id column to patients table and establish foreign key relationship if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'patients' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE patients ADD COLUMN user_id VARCHAR(255);

        ALTER TABLE patients
            ADD CONSTRAINT fk_patients_user
            FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE SET NULL;
    END IF;
END $$;
