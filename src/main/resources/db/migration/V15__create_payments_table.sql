CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(36) PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE,
    amount DECIMAL(15, 0) NOT NULL,
    payment_status VARCHAR(30) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    gateway_transaction_id VARCHAR(100),
    order_ref VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    CONSTRAINT fk_payments_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_payments_order_ref ON payments(order_ref);
CREATE INDEX IF NOT EXISTS idx_payments_appointment_id ON payments(appointment_id);
