CREATE TABLE reserva_posicion (
    id BIGSERIAL PRIMARY KEY,
    posicion_id BIGINT NOT NULL,
    order_id VARCHAR(100) NOT NULL,
    cantidad NUMERIC(19, 8) NOT NULL,
    cantidad_ejecutada NUMERIC(19, 8) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(110) NOT NULL,
    motivo_liberacion VARCHAR(255),
    creado_en TIMESTAMPTZ NOT NULL,
    confirmado_en TIMESTAMPTZ,
    liberado_en TIMESTAMPTZ,
    CONSTRAINT fk_reserva_posicion FOREIGN KEY (posicion_id) REFERENCES posicion (id),
    CONSTRAINT uk_reserva_posicion_order UNIQUE (order_id),
    CONSTRAINT uk_reserva_posicion_clave UNIQUE (idempotency_key),
    CONSTRAINT ck_reserva_posicion_cantidad CHECK (cantidad > 0 AND cantidad_ejecutada >= 0
        AND cantidad_ejecutada <= cantidad),
    CONSTRAINT ck_reserva_posicion_estado CHECK (estado IN ('PENDING', 'CONFIRMED', 'RELEASED'))
);

CREATE INDEX idx_reserva_posicion_posicion ON reserva_posicion (posicion_id, estado);
