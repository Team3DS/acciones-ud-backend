CREATE TABLE posicion (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    cantidad NUMERIC(19, 8) NOT NULL DEFAULT 0,
    cantidad_reservada NUMERIC(19, 8) NOT NULL DEFAULT 0,
    costo_promedio NUMERIC(19, 4) NOT NULL DEFAULT 0,
    precio_actual NUMERIC(19, 4),
    valor_actual NUMERIC(19, 4),
    pnl_no_realizado NUMERIC(19, 4),
    pnl_realizado NUMERIC(19, 4) NOT NULL DEFAULT 0,
    valorado_en TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL,
    actualizado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_posicion_user_symbol UNIQUE (user_id, symbol),
    CONSTRAINT ck_posicion_cantidades CHECK (cantidad >= 0 AND cantidad_reservada >= 0
        AND cantidad_reservada <= cantidad),
    CONSTRAINT ck_posicion_costo_promedio CHECK (costo_promedio >= 0),
    CONSTRAINT ck_posicion_precio_actual CHECK (precio_actual IS NULL OR precio_actual > 0)
);

CREATE INDEX idx_posicion_symbol_abierta ON posicion (symbol) WHERE cantidad > 0;

CREATE TABLE movimiento_posicion (
    id BIGSERIAL PRIMARY KEY,
    posicion_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    cantidad NUMERIC(19, 8) NOT NULL,
    precio NUMERIC(19, 4) NOT NULL,
    execution_id VARCHAR(100),
    order_id VARCHAR(100),
    idempotency_key VARCHAR(110) NOT NULL,
    ocurrido_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_movimiento_posicion FOREIGN KEY (posicion_id) REFERENCES posicion (id),
    CONSTRAINT uk_movimiento_posicion_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_movimiento_posicion_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_movimiento_posicion_precio CHECK (precio > 0),
    CONSTRAINT ck_movimiento_posicion_tipo CHECK (tipo IN ('COMPRA', 'VENTA', 'RESERVA', 'LIBERACION', 'AJUSTE'))
);

CREATE INDEX idx_movimiento_posicion_fecha ON movimiento_posicion (posicion_id, ocurrido_en DESC);
