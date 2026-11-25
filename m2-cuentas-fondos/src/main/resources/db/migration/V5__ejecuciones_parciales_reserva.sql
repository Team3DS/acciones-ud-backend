-- Campos aditivos: no modifica saldos ni reinterpreta ejecuciones históricas.
ALTER TABLE reserva ADD COLUMN monto_consumido NUMERIC(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE reserva ADD CONSTRAINT ck_reserva_consumido_no_negativo CHECK (monto_consumido >= 0);
ALTER TABLE reserva ADD COLUMN ejecuciones_aplicadas INTEGER NOT NULL DEFAULT 0 CHECK (ejecuciones_aplicadas >= 0);
ALTER TABLE reserva ADD COLUMN ejecuciones_esperadas INTEGER CHECK (ejecuciones_esperadas > 0);

CREATE TABLE reserva_ejecucion (
    id BIGSERIAL PRIMARY KEY,
    reserva_id BIGINT NOT NULL REFERENCES reserva(id),
    execution_id VARCHAR(100) NOT NULL,
    monto NUMERIC(19, 2) NOT NULL CHECK (monto > 0),
    final_execution BOOLEAN NOT NULL,
    expected_execution_count INTEGER CHECK (expected_execution_count > 0),
    creado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_reserva_ejecucion UNIQUE (reserva_id, execution_id)
);
