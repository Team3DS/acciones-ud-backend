ALTER TABLE notificacion ADD COLUMN evento_id UUID;
ALTER TABLE notificacion ADD COLUMN visible_app BOOLEAN NOT NULL DEFAULT TRUE;
CREATE UNIQUE INDEX uq_notificacion_evento_usuario ON notificacion(evento_id,usuario_id);
ALTER TABLE alerta_precio ADD COLUMN symbol VARCHAR(20);
ALTER TABLE alerta_precio ADD COLUMN clave_idempotencia VARCHAR(100);
ALTER TABLE alerta_precio ADD COLUMN solicitud_hash VARCHAR(64);
CREATE UNIQUE INDEX uq_alerta_usuario_clave ON alerta_precio(usuario_id,clave_idempotencia);
CREATE TABLE entrega_correo (
 id UUID PRIMARY KEY, usuario_id BIGINT NOT NULL, notificacion_id BIGINT,
 reporte_id BIGINT, contenido_cifrado TEXT NOT NULL,
 estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE', intentos INTEGER NOT NULL DEFAULT 0,
 creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 proximo_intento_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 expira_en TIMESTAMPTZ NOT NULL, enviada_en TIMESTAMPTZ,
 proveedor_id VARCHAR(100), error_codigo VARCHAR(50),
 CONSTRAINT ck_entrega_estado CHECK (estado IN ('PENDIENTE','ENVIADO','FALLIDO','EXPIRADO')),
 CONSTRAINT ck_entrega_intentos CHECK (intentos >= 0)
);
CREATE INDEX ix_entrega_pendiente ON entrega_correo(proximo_intento_en) WHERE estado='PENDIENTE';
DO $$
BEGIN
 IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='avisos_app') THEN
  GRANT SELECT, INSERT, UPDATE ON entrega_correo TO avisos_app;
 END IF;
END
$$;
