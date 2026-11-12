CREATE TABLE cuenta (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    saldo_contable NUMERIC(19, 2) NOT NULL DEFAULT 0,
    saldo_disponible NUMERIC(19, 2) NOT NULL DEFAULT 0,
    saldo_reservado NUMERIC(19, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    creado_en TIMESTAMPTZ NOT NULL,
    actualizado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_cuenta_user_id UNIQUE (user_id),
    CONSTRAINT ck_cuenta_saldos_no_negativos CHECK (saldo_disponible >= 0 AND saldo_reservado >= 0),
    CONSTRAINT ck_cuenta_saldos_coherentes CHECK (saldo_contable = saldo_disponible + saldo_reservado),
    CONSTRAINT ck_cuenta_estado CHECK (estado IN ('ACTIVA', 'BLOQUEADA', 'CERRADA'))
);

CREATE TABLE movimiento (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    monto NUMERIC(19, 2) NOT NULL,
    saldo_posterior NUMERIC(19, 2) NOT NULL,
    referencia VARCHAR(100),
    idempotency_key VARCHAR(100) NOT NULL,
    descripcion TEXT,
    creado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_movimiento_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuenta (id),
    CONSTRAINT uk_movimiento_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_movimiento_monto_positivo CHECK (monto > 0),
    CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('DEPOSITO', 'RETIRO', 'DEBITO_COMPRA', 'CREDITO_VENTA', 'RESERVA', 'LIBERACION', 'AJUSTE'))
);

CREATE INDEX idx_movimiento_cuenta_creado ON movimiento (cuenta_id, creado_en DESC);
