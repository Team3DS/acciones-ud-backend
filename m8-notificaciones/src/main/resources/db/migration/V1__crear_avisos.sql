CREATE TABLE notificacion (
 id BIGINT GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_notificacion PRIMARY KEY,
 usuario_id BIGINT NOT NULL, tipo_evento VARCHAR(30) NOT NULL,
 titulo VARCHAR(150) NOT NULL, mensaje VARCHAR(2000) NOT NULL,
 orden_id BIGINT, mercado_codigo VARCHAR(30),
 creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 leida_en TIMESTAMPTZ, correo_enviado_en TIMESTAMPTZ,
 CONSTRAINT ck_notificacion_tipo_evento CHECK (tipo_evento IN ('CAMBIO_ESTADO_ORDEN','EJECUCION_ORDEN','ACTUALIZACION_SALDO','LIBERACION_FONDOS','APERTURA_MERCADO','CIERRE_MERCADO','CAMBIO_HORARIO_MERCADO','ALERTA_PRECIO','SUSCRIPCION','REPORTE_MENSUAL'))
);
CREATE TABLE preferencia_notificacion (
 usuario_id BIGINT NOT NULL, tipo_evento VARCHAR(30) NOT NULL, canal VARCHAR(20) NOT NULL,
 habilitada BOOLEAN NOT NULL DEFAULT TRUE, actualizada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT pk_preferencia_notificacion PRIMARY KEY (usuario_id,tipo_evento,canal),
 CONSTRAINT ck_preferencia_notificacion_evento CHECK (tipo_evento IN ('CAMBIO_ESTADO_ORDEN','EJECUCION_ORDEN','ACTUALIZACION_SALDO','LIBERACION_FONDOS','APERTURA_MERCADO','CIERRE_MERCADO','CAMBIO_HORARIO_MERCADO','ALERTA_PRECIO','SUSCRIPCION','REPORTE_MENSUAL')),
 CONSTRAINT ck_preferencia_notificacion_canal CHECK (canal IN ('CORREO','APP'))
);
CREATE TABLE suscripcion_aviso_mercado (
 usuario_id BIGINT NOT NULL, mercado_codigo VARCHAR(30) NOT NULL,
 suscrito_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT pk_suscripcion_aviso_mercado PRIMARY KEY (usuario_id,mercado_codigo)
);
CREATE TABLE alerta_precio (
 id BIGINT GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_alerta_precio PRIMARY KEY,
 usuario_id BIGINT NOT NULL, accion_id BIGINT NOT NULL,
 condicion_alerta VARCHAR(30) NOT NULL, precio_umbral NUMERIC(18,4) NOT NULL,
 activa BOOLEAN NOT NULL DEFAULT TRUE, disparada_en TIMESTAMPTZ,
 creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT ck_alerta_precio_condicion CHECK (condicion_alerta IN ('SUPERA','CAE_BAJO')),
 CONSTRAINT ck_alerta_precio_umbral CHECK (precio_umbral > 0)
);
CREATE TABLE reporte_mensual (
 id BIGINT GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_reporte_mensual PRIMARY KEY,
 usuario_id BIGINT NOT NULL, periodo DATE NOT NULL,
 generado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, enviado_en TIMESTAMPTZ,
 CONSTRAINT uq_reporte_mensual_usuario_periodo UNIQUE (usuario_id,periodo)
);
CREATE TABLE inbox_evento (
 evento_id UUID CONSTRAINT pk_inbox_evento PRIMARY KEY,
 servicio_origen VARCHAR(30) NOT NULL, tipo_evento VARCHAR(100) NOT NULL,
 recibido_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, procesado_en TIMESTAMPTZ,
 CONSTRAINT ck_inbox_evento_servicio_origen CHECK (servicio_origen IN ('M1','M2','M4','M5','M10'))
);
CREATE INDEX ix_notificacion_usuario_fecha ON notificacion(usuario_id,creada_en);
CREATE INDEX ix_notificacion_no_leidas ON notificacion(usuario_id) WHERE leida_en IS NULL;
CREATE INDEX ix_notificacion_orden ON notificacion(orden_id);
CREATE INDEX ix_notificacion_mercado ON notificacion(mercado_codigo);
CREATE INDEX ix_suscripcion_aviso_mercado_mercado ON suscripcion_aviso_mercado(mercado_codigo);
CREATE INDEX ix_alerta_precio_usuario ON alerta_precio(usuario_id);
CREATE INDEX ix_alerta_precio_accion_activa ON alerta_precio(accion_id) WHERE activa;
