CREATE TABLE cliente_stripe (
    user_id BIGINT PRIMARY KEY,
    stripe_customer_id VARCHAR(100) NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_cliente_stripe_customer UNIQUE (stripe_customer_id)
);

CREATE TABLE sesion_checkout (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    precio_plan_id BIGINT NOT NULL,
    stripe_session_id VARCHAR(100) NOT NULL,
    url TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL,
    expira_en TIMESTAMPTZ NOT NULL,
    completado_en TIMESTAMPTZ,
    stripe_customer_id VARCHAR(100),
    CONSTRAINT fk_sesion_checkout_precio FOREIGN KEY (precio_plan_id) REFERENCES precio_plan (id),
    CONSTRAINT uk_sesion_checkout_stripe_session UNIQUE (stripe_session_id),
    CONSTRAINT uk_sesion_checkout_user_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_sesion_checkout_estado CHECK (estado IN ('ABIERTA', 'COMPLETADA', 'EXPIRADA'))
);

CREATE INDEX idx_sesion_checkout_user_estado ON sesion_checkout (user_id, estado);

CREATE TABLE suscripcion (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    precio_plan_id BIGINT NOT NULL,
    stripe_subscription_id VARCHAR(100) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    periodo_inicio TIMESTAMPTZ NOT NULL,
    periodo_fin TIMESTAMPTZ NOT NULL,
    cancelar_al_final BOOLEAN NOT NULL,
    cancelada_en TIMESTAMPTZ,
    finalizada_en TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL,
    actualizado_en TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_suscripcion_precio FOREIGN KEY (precio_plan_id) REFERENCES precio_plan (id),
    CONSTRAINT uk_suscripcion_stripe UNIQUE (stripe_subscription_id),
    CONSTRAINT ck_suscripcion_estado CHECK (estado IN ('INCOMPLETA', 'ACTIVA', 'EN_MORA', 'CANCELADA', 'VENCIDA')),
    CONSTRAINT ck_suscripcion_periodo CHECK (periodo_fin > periodo_inicio)
);

CREATE UNIQUE INDEX uk_suscripcion_vigente ON suscripcion (user_id)
    WHERE estado IN ('INCOMPLETA', 'ACTIVA', 'EN_MORA');

CREATE INDEX idx_suscripcion_user_creado ON suscripcion (user_id, creado_en DESC);
