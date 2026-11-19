ALTER TABLE mercado ADD COLUMN pais VARCHAR(50);
ALTER TABLE mercado ADD COLUMN horario_apertura TIME;
ALTER TABLE mercado ADD COLUMN horario_cierre TIME;
ALTER TABLE mercado ADD CONSTRAINT ck_mercado_horario_referencia CHECK ((horario_apertura IS NULL) = (horario_cierre IS NULL));
UPDATE mercado SET pais='Estados Unidos',horario_apertura='09:30',horario_cierre='16:00' WHERE codigo IN ('NYSE','NASDAQ');
ALTER TABLE accion ALTER COLUMN ticker TYPE VARCHAR(20);
ALTER TABLE accion ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'ACCION';
ALTER TABLE accion ADD COLUMN lot_size NUMERIC(19,8) NOT NULL DEFAULT 1;
ALTER TABLE accion ADD COLUMN tick_size NUMERIC(19,8) NOT NULL DEFAULT 0.01;
ALTER TABLE accion ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';
ALTER TABLE accion ADD COLUMN creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE accion ADD COLUMN actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
UPDATE accion SET estado='SUSPENDIDO' WHERE NOT habilitada;
ALTER TABLE accion ADD CONSTRAINT ck_accion_tipo CHECK (tipo IN ('ACCION','ETF','OPCION','FUTURO','BONO','CRYPTO'));
ALTER TABLE accion ADD CONSTRAINT ck_accion_estado CHECK (estado IN ('ACTIVO','SUSPENDIDO','RETIRADO'));
ALTER TABLE accion ADD CONSTRAINT ck_accion_lot_size CHECK (lot_size>0);
ALTER TABLE accion ADD CONSTRAINT ck_accion_tick_size CHECK (tick_size>0);
CREATE INDEX ix_accion_tipo ON accion(tipo);
CREATE TABLE comando_catalogo (
    actor_id BIGINT NOT NULL,
    operacion VARCHAR(150) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    fingerprint CHAR(64) NOT NULL,
    status SMALLINT,
    body JSONB,
    location TEXT,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(actor_id,operacion,idempotency_key)
);
CREATE TABLE outbox_evento (
    id UUID PRIMARY KEY,
    routing_key VARCHAR(100) NOT NULL,
    body JSONB NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    publicado_en TIMESTAMPTZ,
    intentos INTEGER NOT NULL DEFAULT 0,
    proximo_intento TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_outbox_pendiente ON outbox_evento(proximo_intento,creado_en) WHERE publicado_en IS NULL;
