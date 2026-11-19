CREATE TABLE historico_cotizacion (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    open NUMERIC(19,4) NOT NULL,
    high NUMERIC(19,4) NOT NULL,
    low NUMERIC(19,4) NOT NULL,
    close NUMERIC(19,4) NOT NULL,
    volume BIGINT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    intervalo VARCHAR(10) NOT NULL
);

CREATE INDEX idx_historico_symbol_timestamp ON historico_cotizacion (symbol, timestamp DESC);
CREATE INDEX idx_historico_symbol_intervalo_timestamp ON historico_cotizacion (symbol, intervalo, timestamp DESC);

CREATE TABLE horario_mercado (
    id BIGSERIAL PRIMARY KEY,
    mercado_mic VARCHAR(20) NOT NULL,
    dia_semana INT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    apertura TIME NOT NULL,
    cierre TIME NOT NULL,
    pre_market TIME,
    after_hours TIME,
    UNIQUE (mercado_mic, dia_semana)
);

INSERT INTO horario_mercado (mercado_mic, dia_semana, apertura, cierre, pre_market, after_hours) VALUES
('XNYS', 1, '09:30:00', '16:00:00', '04:00:00', '20:00:00'),
('XNYS', 2, '09:30:00', '16:00:00', '04:00:00', '20:00:00'),
('XNYS', 3, '09:30:00', '16:00:00', '04:00:00', '20:00:00'),
('XNYS', 4, '09:30:00', '16:00:00', '04:00:00', '20:00:00'),
('XNYS', 5, '09:30:00', '16:00:00', '04:00:00', '20:00:00'),
('XNAS', 1, '09:30:00', '16:00:00', '04:00:00', '20:00:00'),
('XNAS', 2, '09:30:00', '16:00:00', '04:00:00', '20:00:00'),
('XNAS', 3, '09:30:00', '16:00:00', '04:00:00', '20:00:00'),
('XNAS', 4, '09:30:00', '16:00:00', '04:00:00', '20:00:00'),
('XNAS', 5, '09:30:00', '16:00:00', '04:00:00', '20:00:00');