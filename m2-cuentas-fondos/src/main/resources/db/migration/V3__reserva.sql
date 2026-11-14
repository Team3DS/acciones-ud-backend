CREATE TABLE reserva (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    order_id VARCHAR(100) NOT NULL,
    monto NUMERIC(19, 2) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    execution_id VARCHAR(100),
    motivo_liberacion VARCHAR(255),
    creado_en TIMESTAMPTZ NOT NULL,
    confirmado_en TIMESTAMPTZ,
    liberado_en TIMESTAMPTZ,
    CONSTRAINT fk_reserva_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuenta (id),
    CONSTRAINT uk_reserva_order_id UNIQUE (order_id),
    CONSTRAINT uk_reserva_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_reserva_monto_positivo CHECK (monto > 0),
    CONSTRAINT ck_reserva_estado CHECK (estado IN ('PENDING', 'CONFIRMED', 'RELEASED', 'EXPIRED'))
);

CREATE INDEX idx_reserva_estado_creado ON reserva (estado, creado_en);
