CREATE TABLE evento_stripe (
    id VARCHAR(100) PRIMARY KEY,
    tipo VARCHAR(100) NOT NULL,
    objeto_id VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    recibido_en TIMESTAMPTZ NOT NULL,
    procesado_en TIMESTAMPTZ,
    intentos INTEGER NOT NULL DEFAULT 0,
    ultimo_error TEXT,
    siguiente_intento_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_evento_stripe_intentos CHECK (intentos >= 0)
);

CREATE INDEX idx_evento_stripe_pendientes ON evento_stripe (siguiente_intento_en, recibido_en)
    WHERE procesado_en IS NULL;
