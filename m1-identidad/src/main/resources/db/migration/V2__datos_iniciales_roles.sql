INSERT INTO rol (nombre, descripcion) VALUES
    ('ADMINISTRADOR', 'Administrador del sistema con acceso total'),
    ('DIRECTIVO', 'Directivo con acceso a reportes y supervisión'),
    ('COMISIONISTA', 'Comisionista con acceso a operaciones de comisiones'),
    ('CUMPLIMIENTO', 'Encargado de cumplimiento normativo'),
    ('INVERSIONISTA', 'Usuario inversionista con acceso a portafolio y órdenes')
ON CONFLICT (nombre) DO NOTHING;