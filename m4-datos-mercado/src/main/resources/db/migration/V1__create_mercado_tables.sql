CREATE TABLE IF NOT EXISTS historico_cotizacion (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    open NUMERIC(19,4) NOT NULL,
    high NUMERIC(19,4) NOT NULL,
    low NUMERIC(19,4) NOT NULL,
    close NUMERIC(19,4) NOT NULL,
    volume BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    intervalo VARCHAR(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS horario_mercado (
    id BIGSERIAL PRIMARY KEY,
    mercado_mic VARCHAR(20) NOT NULL,
    dia_semana INT NOT NULL,
    apertura TIME NOT NULL,
    cierre TIME NOT NULL,
    pre_market TIME,
    after_hours TIME
);
