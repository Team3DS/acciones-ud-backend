CREATE TABLE plan (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    activo BOOLEAN NOT NULL,
    CONSTRAINT uk_plan_codigo UNIQUE (codigo)
);

CREATE TABLE precio_plan (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    intervalo VARCHAR(10) NOT NULL,
    monto NUMERIC(19, 2) NOT NULL,
    moneda CHAR(3) NOT NULL,
    stripe_lookup_key VARCHAR(100) NOT NULL,
    activo BOOLEAN NOT NULL,
    CONSTRAINT fk_precio_plan_plan FOREIGN KEY (plan_id) REFERENCES plan (id),
    CONSTRAINT uk_precio_plan_lookup_key UNIQUE (stripe_lookup_key),
    CONSTRAINT uk_precio_plan_plan_intervalo UNIQUE (plan_id, intervalo),
    CONSTRAINT ck_precio_plan_intervalo CHECK (intervalo IN ('MENSUAL', 'ANUAL')),
    CONSTRAINT ck_precio_plan_monto CHECK (monto > 0),
    CONSTRAINT ck_precio_plan_moneda CHECK (moneda = 'USD')
);

INSERT INTO plan (codigo, nombre, descripcion, activo)
VALUES ('PREMIUM', 'Acciones UD Premium', 'Watchlist y alertas de precio', TRUE);

INSERT INTO precio_plan (plan_id, intervalo, monto, moneda, stripe_lookup_key, activo)
SELECT id, 'MENSUAL', 12.00, 'USD', 'acciones_ud_premium_mensual', TRUE
FROM plan
WHERE codigo = 'PREMIUM';

INSERT INTO precio_plan (plan_id, intervalo, monto, moneda, stripe_lookup_key, activo)
SELECT id, 'ANUAL', 120.00, 'USD', 'acciones_ud_premium_anual', TRUE
FROM plan
WHERE codigo = 'PREMIUM';
