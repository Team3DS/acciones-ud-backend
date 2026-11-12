CREATE TABLE outbox_evento (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    correlation_id VARCHAR(100),
    creado_en TIMESTAMPTZ NOT NULL,
    procesado_en TIMESTAMPTZ,
    publicado BOOLEAN NOT NULL DEFAULT FALSE,
    reintentos INT NOT NULL DEFAULT 0,
    ultimo_error TEXT,
    CONSTRAINT ck_outbox_reintentos_no_negativos CHECK (reintentos >= 0)
);

CREATE INDEX idx_outbox_pendientes ON outbox_evento (creado_en) WHERE publicado = FALSE;
