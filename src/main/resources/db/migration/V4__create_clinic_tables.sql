CREATE TABLE IF NOT EXISTS category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    price NUMERIC(19, 2),
    status VARCHAR(255),
    category_id BIGINT REFERENCES category(id)
);

CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    citizen_id VARCHAR(12) UNIQUE,
    created_at DATE
);

CREATE TABLE IF NOT EXISTS checkups (
    id BIGSERIAL PRIMARY KEY,
    diagnoses VARCHAR(255),
    patient_id BIGINT REFERENCES patients(id)
);
