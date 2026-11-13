CREATE TABLE mercado (
    id SMALLINT GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_mercado PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL CONSTRAINT uq_mercado_codigo UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    codigo_mic CHAR(4) NOT NULL CONSTRAINT uq_mercado_codigo_mic UNIQUE,
    zona_horaria VARCHAR(64) NOT NULL,
    moneda CHAR(3) NOT NULL DEFAULT 'USD' CONSTRAINT ck_mercado_moneda CHECK (moneda = 'USD'),
    habilitado BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE sector (
    id SMALLINT GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_sector PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL CONSTRAINT uq_sector_codigo UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255)
);
CREATE TABLE accion (
    id BIGINT GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_accion PRIMARY KEY,
    mercado_id SMALLINT NOT NULL CONSTRAINT fk_accion_mercado REFERENCES mercado(id),
    sector_id SMALLINT NOT NULL CONSTRAINT fk_accion_sector REFERENCES sector(id),
    ticker VARCHAR(10) NOT NULL,
    nombre_empresa VARCHAR(150) NOT NULL,
    habilitada BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_accion_mercado_ticker UNIQUE (mercado_id, ticker)
);
CREATE INDEX ix_accion_sector ON accion(sector_id);
CREATE INDEX ix_accion_ticker ON accion(ticker);
