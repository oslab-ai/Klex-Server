#!/bin/bash
set -e

echo "🚀 Klex Unified Backend — Starting..."

# ── Auto-derive Spring datasource vars from DATABASE_URL ──────
# This allows the Java jar to connect to the same DB as Django,
# using the single DATABASE_URL from .env — no Java config needed.
if [ -n "${DATABASE_URL:-}" ] && [ -z "${SPRING_DATASOURCE_URL:-}" ]; then
    DB_URL="$DATABASE_URL"
    DB_URL="${DB_URL#postgres://}"          # strip scheme
    DB_USERPASS="${DB_URL%%@*}"             # user:pass
    DB_HOSTDB="${DB_URL#*@}"               # host:port/db
    DB_USER="${DB_USERPASS%%:*}"
    DB_PASS="${DB_USERPASS#*:}"
    export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOSTDB}"
    export SPRING_DATASOURCE_USERNAME="$DB_USER"
    export SPRING_DATASOURCE_PASSWORD="$DB_PASS"
    echo "📎 Auto-derived SPRING_DATASOURCE_URL=$SPRING_DATASOURCE_URL"
fi

# Run Django migrations
echo "📦 Running Django migrations..."
cd /app
.venv/bin/python backend/manage.py migrate --noinput 2>&1 || echo "⚠️  Migrations failed (DB might not be ready yet)"

# ── Scheduler Engine Configuration ──────────────────────────────
export ENV_AIRFLOW_AUTOSTART="false"
ACTIVE_ENGINE="quartz"

if [ -f "/app/backend/scheduler_config.yaml" ]; then
    if grep -q 'active_engine:[[:space:]]*"airflow"' /app/backend/scheduler_config.yaml; then
        ACTIVE_ENGINE="airflow"
        export ENV_AIRFLOW_AUTOSTART="true"
    fi
fi

echo "📅 Active Scheduler Engine: $ACTIVE_ENGINE"

if [ "$ACTIVE_ENGINE" = "airflow" ]; then
    echo "🌬️  Initializing Airflow..."
    export AIRFLOW_HOME="/app/backend/airflow_home"
    mkdir -p "$AIRFLOW_HOME/dags" "$AIRFLOW_HOME/logs"

    # Initialize / migrate Airflow DB
    /app/.venv/bin/airflow db migrate 2>&1 | tail -1

    # Ensure admin user exists
    AIRFLOW_ADMIN_USER="${AIRFLOW_USERNAME:-airflow}"
    AIRFLOW_ADMIN_PASS="${AIRFLOW_PASSWORD:-airflow}"
    
    if ! /app/.venv/bin/airflow users list 2>/dev/null | grep -q "$AIRFLOW_ADMIN_USER"; then
        echo "🌬️  Creating Airflow admin user '${AIRFLOW_ADMIN_USER}'..."
        /app/.venv/bin/airflow users create \
            --username "$AIRFLOW_ADMIN_USER" \
            --password "$AIRFLOW_ADMIN_PASS" \
            --firstname Admin \
            --lastname User \
            --role Admin \
            --email admin@klex.local 2>&1 | tail -1
    fi
fi

echo "✅ Migrations complete. Starting services via supervisord..."

# Hand off to supervisord (CMD)
exec "$@"
