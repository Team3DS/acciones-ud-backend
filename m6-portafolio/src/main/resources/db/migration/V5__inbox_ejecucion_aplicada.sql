CREATE TABLE inbox_evento (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100),
    correlation_id VARCHAR(100),
    payload JSONB,
    recibido_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ejecucion_aplicada (
    id BIGSERIAL PRIMARY KEY,
    posicion_id BIGINT NOT NULL,
    ejecucion_orden_id BIGINT NOT NULL,
    lado_orden VARCHAR(10) NOT NULL,
    cantidad NUMERIC(19, 8) NOT NULL,
    precio_ejecucion NUMERIC(19, 4) NOT NULL,
    precio_promedio_costo NUMERIC(19, 4) NOT NULL,
    clave_idempotencia UUID NOT NULL,
    aplicado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ejecucion_aplicada_posicion FOREIGN KEY (posicion_id) REFERENCES posicion (id),
    CONSTRAINT uk_ejecucion_aplicada_orden UNIQUE (ejecucion_orden_id),
    CONSTRAINT uk_ejecucion_aplicada_clave UNIQUE (clave_idempotencia),
    CONSTRAINT ck_ejecucion_aplicada_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_ejecucion_aplicada_precio CHECK (precio_ejecucion > 0 AND precio_promedio_costo > 0),
    CONSTRAINT ck_ejecucion_aplicada_lado CHECK (lado_orden IN ('BUY', 'SELL'))
);

CREATE INDEX idx_ejecucion_aplicada_posicion ON ejecucion_aplicada (posicion_id);
CREATE INDEX idx_inbox_evento_tipo ON inbox_evento (event_type);