#!/bin/bash
set -e

# Runs once, on first startup of an empty mysql-db volume (mounted into
# /docker-entrypoint-initdb.d by docker-compose.yml). Creates the three
# per-service databases and their service-specific credentials inside the
# ONE MySQL instance. Each service still only has grants on its own
# database - this is one shared MySQL server, not one shared schema.
mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
    CREATE DATABASE IF NOT EXISTS authentication_db;
    CREATE DATABASE IF NOT EXISTS user_profile_db;
    CREATE DATABASE IF NOT EXISTS notification_db;

    CREATE USER IF NOT EXISTS '${AUTH_DB_USERNAME}'@'%' IDENTIFIED BY '${AUTH_DB_PASSWORD}';
    GRANT ALL PRIVILEGES ON authentication_db.* TO '${AUTH_DB_USERNAME}'@'%';

    CREATE USER IF NOT EXISTS '${PROFILE_DB_USERNAME}'@'%' IDENTIFIED BY '${PROFILE_DB_PASSWORD}';
    GRANT ALL PRIVILEGES ON user_profile_db.* TO '${PROFILE_DB_USERNAME}'@'%';

    CREATE USER IF NOT EXISTS '${NOTIFICATION_DB_USERNAME}'@'%' IDENTIFIED BY '${NOTIFICATION_DB_PASSWORD}';
    GRANT ALL PRIVILEGES ON notification_db.* TO '${NOTIFICATION_DB_USERNAME}'@'%';

    FLUSH PRIVILEGES;
EOSQL
