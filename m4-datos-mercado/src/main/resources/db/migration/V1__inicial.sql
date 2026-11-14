CREATE TABLE IF NOT EXISTS historico_cotizacion (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    open NUMERIC(19, 4) NOT NULL,
    high NUMERIC(19, 4) NOT NULL,
    low NUMERIC(19, 4) NOT NULL,
    close NUMERIC(19, 4) NOT NULL,
    volume BIGINT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    intervalo VARCHAR(10) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_historico_symbol_timestamp ON historico_cotizacion(symbol, timestamp);