-- V1__crear_tabla_ordenes.sql

CREATE TABLE IF NOT EXISTS orden (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    simbolo VARCHAR(20) NOT NULL,
    mercado VARCHAR(30) NOT NULL DEFAULT 'NYSE',
    lado VARCHAR(20) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    cantidad NUMERIC(19,4) NOT NULL,
    precio_limite NUMERIC(19,4),
    precio_ejecutado NUMERIC(19,4),
    comision NUMERIC(19,4),
    valor_total NUMERIC(19,4),
    estado VARCHAR(30) NOT NULL,
    client_order_id VARCHAR(100),
    orden_externa_id VARCHAR(100),
    reserva_fondos_id VARCHAR(100),
    reserva_posicion_id VARCHAR(100),
    creado_en TIMESTAMP,
    actualizado_en TIMESTAMP,
    enviada_en TIMESTAMP,
    ejecutada_en TIMESTAMP,
    cantidad_ejecutada INTEGER,
    cantidad_remanente INTEGER
);

CREATE INDEX IF NOT EXISTS idx_orden_usuario_id ON orden(usuario_id);
CREATE INDEX IF NOT EXISTS idx_orden_estado ON orden(estado);
CREATE INDEX IF NOT EXISTS idx_orden_client_order_id ON orden(client_order_id);
CREATE INDEX IF NOT EXISTS idx_orden_simbolo ON orden(simbolo);
