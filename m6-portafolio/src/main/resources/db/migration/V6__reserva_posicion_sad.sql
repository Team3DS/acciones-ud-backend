ALTER TABLE reserva_posicion
    RENAME COLUMN order_id TO orden_id;

ALTER TABLE reserva_posicion
    ALTER COLUMN orden_id SET DATA TYPE BIGINT;

ALTER TABLE reserva_posicion
    RENAME COLUMN cantidad TO cantidad_reservada;

ALTER TABLE reserva_posicion
    ALTER COLUMN cantidad_reservada SET DATA TYPE INTEGER;

ALTER TABLE reserva_posicion
    RENAME COLUMN cantidad_ejecutada TO cantidad_consumida;

ALTER TABLE reserva_posicion
    ALTER COLUMN cantidad_consumida SET DATA TYPE INTEGER;

ALTER TABLE reserva_posicion
    ADD COLUMN cantidad_liberada INTEGER DEFAULT 0;

ALTER TABLE reserva_posicion
    RENAME COLUMN estado TO estado_reserva;

ALTER TABLE reserva_posicion
    ALTER COLUMN estado_reserva SET DATA TYPE VARCHAR(30);

UPDATE reserva_posicion
SET estado_reserva = CASE
    WHEN estado_reserva = 'PENDING' THEN 'ACTIVA'
    WHEN estado_reserva = 'CONFIRMED' THEN 'CONSUMIDA'
    WHEN estado_reserva = 'RELEASED' THEN 'LIBERADA'
    ELSE 'ACTIVA'
END;

ALTER TABLE reserva_posicion
    ALTER COLUMN estado_reserva SET NOT NULL;

ALTER TABLE reserva_posicion
    RENAME COLUMN idempotency_key TO clave_idempotencia;

ALTER TABLE reserva_posicion
    ALTER COLUMN clave_idempotencia SET DATA TYPE UUID;

ALTER TABLE reserva_posicion
    RENAME COLUMN creado_en TO creada_en;

ALTER TABLE reserva_posicion
    DROP COLUMN confirmado_en;

ALTER TABLE reserva_posicion
    DROP CONSTRAINT uk_reserva_posicion_order,
    DROP CONSTRAINT uk_reserva_posicion_clave,
    DROP CONSTRAINT ck_reserva_posicion_cantidad,
    DROP CONSTRAINT ck_reserva_posicion_estado;

ALTER TABLE reserva_posicion
    ADD CONSTRAINT uk_reserva_posicion_orden UNIQUE (orden_id),
    ADD CONSTRAINT uk_reserva_posicion_clave UNIQUE (clave_idempotencia),
    ADD CONSTRAINT ck_reserva_posicion_cantidades CHECK (
        cantidad_reservada > 0
        AND cantidad_consumida >= 0
        AND cantidad_liberada >= 0
        AND (cantidad_consumida + cantidad_liberada) <= cantidad_reservada
    ),
    ADD CONSTRAINT ck_reserva_posicion_estado CHECK (
        estado_reserva IN ('ACTIVA', 'CONSUMIDA', 'LIBERADA')
    );

DROP INDEX IF EXISTS idx_reserva_posicion_posicion;
CREATE INDEX idx_reserva_posicion_posicion ON reserva_posicion (posicion_id, estado_reserva);