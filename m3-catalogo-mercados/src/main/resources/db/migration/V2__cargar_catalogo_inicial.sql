INSERT INTO MERCADO (codigo, nombre, codigo_mic, zona_horaria, moneda, habilitado) VALUES
    ('NYSE',   'New York Stock Exchange', 'XNYS', 'America/New_York', 'USD', TRUE),
    ('NASDAQ', 'Nasdaq Stock Market',     'XNAS', 'America/New_York', 'USD', TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO SECTOR (codigo, nombre, descripcion) VALUES
    ('TECNOLOGIA',             'Tecnología',                'Software, hardware y semiconductores.'),
    ('SERVICIOS_COMUNICACION', 'Servicios de comunicación', 'Internet, medios y plataformas de comunicación.'),
    ('CONSUMO_DISCRECIONAL',   'Consumo discrecional',      'Comercio minorista, automotriz y bienes de consumo no esencial.'),
    ('FINANCIERO',             'Financiero',                'Bancos, seguros y servicios financieros.'),
    ('SALUD',                  'Salud',                     'Farmacéuticas, dispositivos y servicios de salud.'),
    ('ENERGIA',                'Energía',                   'Petróleo, gas y energía.')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO ACCION (mercado_id, sector_id, ticker, nombre_empresa, habilitada)
SELECT m.id, s.id, v.ticker, v.nombre_empresa, TRUE
FROM (VALUES
    ('NYSE',   'SALUD',                  'JNJ',   'Johnson & Johnson'),
    ('NYSE',   'FINANCIERO',             'JPM',   'JPMorgan Chase & Co.'),
    ('NYSE',   'ENERGIA',                'XOM',   'Exxon Mobil Corporation'),
    ('NASDAQ', 'TECNOLOGIA',             'AAPL',  'Apple Inc.'),
    ('NASDAQ', 'TECNOLOGIA',             'MSFT',  'Microsoft Corporation'),
    ('NASDAQ', 'SERVICIOS_COMUNICACION', 'GOOGL', 'Alphabet Inc.'),
    ('NASDAQ', 'CONSUMO_DISCRECIONAL',   'AMZN',  'Amazon.com, Inc.'),
    ('NASDAQ', 'SERVICIOS_COMUNICACION', 'META',  'Meta Platforms, Inc.'),
    ('NASDAQ', 'CONSUMO_DISCRECIONAL',   'TSLA',  'Tesla, Inc.'),
    ('NASDAQ', 'TECNOLOGIA',             'NVDA',  'NVIDIA Corporation')
) AS v(mercado, sector, ticker, nombre_empresa)
JOIN MERCADO m ON m.codigo = v.mercado
JOIN SECTOR s ON s.codigo = v.sector
ON CONFLICT (mercado_id, ticker) DO NOTHING;
