ALTER TABLE ejecucion_aplicada
    ALTER COLUMN ejecucion_orden_id TYPE VARCHAR(100)
    USING ejecucion_orden_id::text;

ALTER TABLE ejecucion_aplicada
    DROP CONSTRAINT ck_ejecucion_aplicada_precio;

ALTER TABLE ejecucion_aplicada
    ADD CONSTRAINT ck_ejecucion_aplicada_precio
    CHECK (precio_ejecucion > 0 AND precio_promedio_costo >= 0);

ALTER TABLE ejecucion_aplicada
    DROP CONSTRAINT uk_ejecucion_aplicada_orden;
