-- V1__esquema_inicial_m5_ordenes.sql
-- Alineado al SAD v0.2 Tablas 113 a 118 para M5 Ordenes y Ejecucion

CREATE TABLE IF NOT EXISTS configuracion_comision (
    id BIGSERIAL PRIMARY KEY,
    mercado_codigo VARCHAR(30) NOT NULL,
    porcentaje NUMERIC(7,4) NOT NULL CHECK (porcentaje >= 0 AND porcentaje <= 100),
    vigente_desde TIMESTAMPTZ NOT NULL,
    vigente_hasta TIMESTAMPTZ,
    motivo VARCHAR(500) NOT NULL,
    creado_por_usuario_id BIGINT,
    creado_en TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cfg_comision_mercado ON configuracion_comision(mercado_codigo);
CREATE INDEX IF NOT EXISTS idx_cfg_comision_vigencia ON configuracion_comision(vigente_desde, vigente_hasta);

-- Datos iniciales de comision
INSERT INTO configuracion_comision (id, mercado_codigo, porcentaje, vigente_desde, vigente_hasta, motivo, creado_en)
VALUES (1, 'NYSE', 0.0020, '2026-01-01 00:00:00Z', NULL, 'Comision base NYSE 0.20%', NOW()),
       (2, 'NASDAQ', 0.0020, '2026-01-01 00:00:00Z', NULL, 'Comision base NASDAQ 0.20%', NOW()),
       (3, 'BVC', 0.0050, '2026-01-01 00:00:00Z', NULL, 'Comision base BVC 0.50%', NOW())
ON CONFLICT (id) DO NOTHING;

SELECT setval('configuracion_comision_id_seq', (SELECT MAX(id) FROM configuracion_comision));

CREATE TABLE IF NOT EXISTS orden (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    accion_id BIGINT NOT NULL,
    ticker VARCHAR(10) NOT NULL,
    tipo_orden VARCHAR(30) NOT NULL CHECK (tipo_orden IN ('MARKET', 'LIMIT', 'STOP_LOSS', 'TAKE_PROFIT')),
    lado_orden VARCHAR(30) NOT NULL CHECK (lado_orden IN ('COMPRA', 'VENTA')),
    vigencia_orden VARCHAR(30) NOT NULL CHECK (vigencia_orden IN ('DIA', 'HASTA_CANCELACION')),
    estado_orden VARCHAR(30) NOT NULL CHECK (estado_orden IN ('PENDIENTE', 'ENVIADA', 'FIRMADA_COMISIONISTA', 'EN_EJECUCION', 'PARCIALMENTE_EJECUTADA', 'EJECUTADA', 'CANCELADA', 'RECHAZADA', 'EXPIRADA', 'EN_REVISION', 'FALLIDA', 'DETENIDA')),
    configuracion_comision_id BIGINT NOT NULL REFERENCES configuracion_comision(id),
    cantidad INTEGER NOT NULL CHECK (cantidad >= 1),
    precio_limite NUMERIC(18,4) CHECK (precio_limite IS NULL OR precio_limite > 0),
    precio_activacion NUMERIC(18,4) CHECK (precio_activacion IS NULL OR precio_activacion > 0),
    precio_estimado NUMERIC(18,4) NOT NULL CHECK (precio_estimado > 0),
    requiere_encolamiento BOOLEAN NOT NULL DEFAULT FALSE,
    confirmado_encolamiento_en TIMESTAMPTZ,
    client_order_id UUID NOT NULL UNIQUE,
    broker_order_id VARCHAR(64) UNIQUE,
    reserva_fondos_id BIGINT UNIQUE,
    reserva_posicion_id BIGINT UNIQUE,
    expira_en TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    creada_en TIMESTAMPTZ NOT NULL,
    actualizada_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_orden_precios_tipo CHECK (
        (tipo_orden = 'MARKET' AND precio_limite IS NULL AND precio_activacion IS NULL) OR
        (tipo_orden = 'LIMIT' AND precio_limite IS NOT NULL AND precio_activacion IS NULL) OR
        (tipo_orden IN ('STOP_LOSS', 'TAKE_PROFIT') AND precio_activacion IS NOT NULL)
    ),
    CONSTRAINT ck_orden_reserva_lado CHECK (
        (lado_orden = 'COMPRA' AND reserva_posicion_id IS NULL) OR
        (lado_orden = 'VENTA' AND reserva_fondos_id IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_orden_usuario_id ON orden(usuario_id);
CREATE INDEX IF NOT EXISTS idx_orden_estado ON orden(estado_orden);
CREATE INDEX IF NOT EXISTS idx_orden_client_order_id ON orden(client_order_id);
CREATE INDEX IF NOT EXISTS idx_orden_broker_order_id ON orden(broker_order_id);
CREATE INDEX IF NOT EXISTS idx_orden_ticker ON orden(ticker);

CREATE TABLE IF NOT EXISTS historial_estado_orden (
    id BIGSERIAL PRIMARY KEY,
    orden_id BIGINT NOT NULL REFERENCES orden(id),
    estado_orden VARCHAR(30) NOT NULL CHECK (estado_orden IN ('PENDIENTE', 'ENVIADA', 'FIRMADA_COMISIONISTA', 'EN_EJECUCION', 'PARCIALMENTE_EJECUTADA', 'EJECUTADA', 'CANCELADA', 'RECHAZADA', 'EXPIRADA', 'EN_REVISION', 'FALLIDA', 'DETENIDA')),
    tipo_actor VARCHAR(30) NOT NULL CHECK (tipo_actor IN ('USUARIO', 'ADMINISTRADOR', 'SISTEMA', 'BROKER')),
    actor_usuario_id BIGINT,
    causa VARCHAR(500) NOT NULL,
    ocurrido_en TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_historial_orden_id ON historial_estado_orden(orden_id);

CREATE TABLE IF NOT EXISTS ejecucion_orden (
    id BIGSERIAL PRIMARY KEY,
    orden_id BIGINT NOT NULL REFERENCES orden(id),
    broker_execution_id VARCHAR(64) NOT NULL UNIQUE,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    precio_ejecucion NUMERIC(18,4) NOT NULL CHECK (precio_ejecucion > 0),
    comision NUMERIC(18,2) NOT NULL CHECK (comision >= 0),
    ejecutada_en TIMESTAMPTZ NOT NULL,
    registrada_en TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ejecucion_orden_id ON ejecucion_orden(orden_id);
CREATE INDEX IF NOT EXISTS idx_ejecucion_broker_exec_id ON ejecucion_orden(broker_execution_id);

CREATE TABLE IF NOT EXISTS confirmacion_ejecucion (
    ejecucion_orden_id BIGINT NOT NULL REFERENCES ejecucion_orden(id),
    servicio_participante VARCHAR(30) NOT NULL CHECK (servicio_participante IN ('FONDOS', 'PORTAFOLIO')),
    precio_promedio_costo NUMERIC(18,4),
    confirmada_en TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (ejecucion_orden_id, servicio_participante),
    CONSTRAINT ck_confirmacion_costo CHECK (
        (servicio_participante = 'PORTAFOLIO' AND precio_promedio_costo IS NOT NULL AND precio_promedio_costo > 0) OR
        (servicio_participante = 'FONDOS' AND precio_promedio_costo IS NULL)
    )
);

CREATE TABLE IF NOT EXISTS outbox_evento (
    id UUID PRIMARY KEY,
    tipo_agregado VARCHAR(50) NOT NULL,
    agregado_id VARCHAR(64) NOT NULL,
    tipo_evento VARCHAR(100) NOT NULL,
    carga JSONB NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL,
    publicado_en TIMESTAMPTZ,
    intentos INTEGER NOT NULL DEFAULT 0 CHECK (intentos >= 0),
    ultimo_error VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_outbox_pendientes ON outbox_evento(creado_en) WHERE publicado_en IS NULL;
