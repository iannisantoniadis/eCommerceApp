#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
    -- Create databases only if they don't exist
    SELECT 'CREATE DATABASE products' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'products')\gexec
    SELECT 'CREATE DATABASE orders' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'orders')\gexec
    SELECT 'CREATE DATABASE payments' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payments')\gexec
    SELECT 'CREATE DATABASE keycloak' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'keycloak')\gexec
EOSQL

echo "Databases setup complete!"