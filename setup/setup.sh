#!/bin/sh

set eux

DATABASE=${DATABASE:-protoquill_test}
HOST=${HOST:-127.0.0.1}

# verify mysql connection
mysql --protocol=tcp -h "$HOST" -u root -e "select 1" > /dev/null

# db migration
mysql --protocol=tcp -h "$HOST" -u root -e "DROP DATABASE $DATABASE" || true
mysql --protocol=tcp -h "$HOST" -u root -e "CREATE DATABASE $DATABASE"
mysql --protocol=tcp -h "$HOST" -u root "$DATABASE" < setup/schema.sql
