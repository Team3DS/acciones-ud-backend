#!/bin/bash
# Creates multiple databases for microservices
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE identidad_db;
    CREATE DATABASE fondos_db;
    CREATE DATABASE catalogo_db;
    CREATE DATABASE mercado_db;
    CREATE DATABASE ordenes_db;
    CREATE DATABASE portafolio_db;
    CREATE DATABASE avisos_db;
    CREATE DATABASE pagos_db;
EOSQL