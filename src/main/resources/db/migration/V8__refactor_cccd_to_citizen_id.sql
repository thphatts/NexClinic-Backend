-- Migration V8: Refactor cccd to citizen_id in users table and add user_id link to patients table

-- 1. Rename column cccd to citizen_id in users table
ALTER TABLE users RENAME COLUMN cccd TO citizen_id;

-- 2. Add user_id column to patients table and establish foreign key relationship
ALTER TABLE patients ADD COLUMN user_id VARCHAR(255);

ALTER TABLE patients
    ADD CONSTRAINT fk_patients_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE SET NULL;
