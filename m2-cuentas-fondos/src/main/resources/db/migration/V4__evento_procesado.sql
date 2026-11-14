CREATE TABLE evento_procesado (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    procesado_en TIMESTAMPTZ NOT NULL
);
