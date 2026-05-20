#!/bin/bash
# Klex Startup Script
# Runs all services in development mode:
#   - Django backend
#   - Vite frontend
#   - Java Reporting Service
#   - Apache Airflow (scheduler + webserver + dispatch/reconcile loops)

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 Starting Klex Development Environment${NC}"
echo -e "   This will start: Django backend, Vite frontend, Reporting Service"

# Determine scheduler engine
ACTIVE_ENGINE="quartz"
if [ -f "backend/scheduler_config.yaml" ]; then
    # Simple extraction using awk
    if grep -q 'active_engine:[[:space:]]*"airflow"' backend/scheduler_config.yaml; then
        ACTIVE_ENGINE="airflow"
    fi
fi

if [ "$ACTIVE_ENGINE" = "airflow" ]; then
    echo -e "   ${CYAN}Scheduler engine: Apache Airflow${NC}"
else
    echo -e "   ${CYAN}Scheduler engine: Quartz (built-in)${NC}"
fi

# Check for .env file
if [ ! -f ".env" ]; then
    echo -e "${YELLOW}⚠️  No .env file found. Copying from .env.example...${NC}"
    if [ -f ".env.example" ]; then
        cp .env.example .env
        echo -e "${YELLOW}   Please edit .env with your configuration before proceeding.${NC}"
        echo -e "${YELLOW}   Required: GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, DJANGO_SECRET_KEY${NC}"
    else
        echo -e "${RED}❌ .env.example not found. Please create a .env file.${NC}"
        exit 1
    fi
fi

# Load .env into current shell so all child processes inherit it
set -a
source .env
set +a

# ── Auto-derive Spring datasource vars from DATABASE_URL ──────
# This lets Java services connect to the same DB as Django without
# the user needing to know anything about Spring configuration.
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
    echo -e "${GREEN}📎 Auto-derived Spring datasource from DATABASE_URL${NC}"
fi

# Check for uv
if ! command -v uv &> /dev/null; then
    echo -e "${RED}❌ uv is not installed.${NC}"
    echo -e "${YELLOW}   Install it with: curl -LsSf https://astral.sh/uv/install.sh | sh${NC}"
    echo -e "${YELLOW}   Or visit: https://docs.astral.sh/uv/getting-started/installation/${NC}"
    exit 1
fi

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${YELLOW}⚠️  Maven is not installed. Java services will not start.${NC}"
    echo -e "${YELLOW}   Install Maven to run the reporting-service and scheduler.${NC}"
    MAVEN_AVAILABLE=false
else
    MAVEN_AVAILABLE=true
fi

# Check for node/npm
if ! command -v npm &> /dev/null; then
    echo -e "${RED}❌ npm is not installed. Please install Node.js.${NC}"
    exit 1
fi

# Function to cleanup background processes on exit
cleanup() {
    echo -e "\n${YELLOW}🛑 Shutting down...${NC}"
    kill $BACKEND_PID 2>/dev/null || true
    kill $FRONTEND_PID 2>/dev/null || true
    kill $REPORTING_PID 2>/dev/null || true
    kill $AIRFLOW_WEB_PID 2>/dev/null || true
    kill $AIRFLOW_SCHED_PID 2>/dev/null || true
    kill $AIRFLOW_DAGPROC_PID 2>/dev/null || true
    kill $DISPATCH_PID 2>/dev/null || true
    kill $RECONCILE_PID 2>/dev/null || true
    exit 0
}

trap cleanup SIGINT SIGTERM

# Backend setup
echo -e "${GREEN}📦 Setting up backend...${NC}"
cd backend

# Sync Python dependencies (include airflow extra so it's always available for migration)
echo -e "   Installing Python dependencies with uv..."
uv sync --extra airflow

# Run migrations
echo -e "   Running database migrations..."
uv run python manage.py migrate

# Create media directory for sample PDF
mkdir -p media

# Start backend server
echo -e "${GREEN}🔧 Starting Django backend on http://localhost:8000${NC}"
uv run python manage.py runserver 0.0.0.0:8000 &
BACKEND_PID=$!

cd ..

# Frontend setup
echo -e "${GREEN}📦 Setting up frontend...${NC}"
cd frontend

# Install npm dependencies
echo -e "   Installing npm dependencies..."
npm install

# Start frontend server
echo -e "${GREEN}⚛️  Starting Vite frontend on http://localhost:5173${NC}"
npm run dev &
FRONTEND_PID=$!

cd ..

# Java Services
REPORTING_PID=""

if [ "$MAVEN_AVAILABLE" = true ]; then
    # Start Reporting Service (local)
    Klex_DIR="services/reporting-service"
    if [ -d "$Klex_DIR" ] && [ -f "$Klex_DIR/pom.xml" ]; then
        echo -e "${GREEN}☕ Starting Reporting Service on http://localhost:8081${NC}"
        cd "$Klex_DIR"
        mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081 -q &
        REPORTING_PID=$!
        cd - > /dev/null
    else
        echo -e "${YELLOW}⚠️  No reporting service found at $Klex_DIR. Skipping.${NC}"
    fi
fi

# ═══════════════════════════════════════════════════════════════
#  Apache Airflow (only when active_engine = "airflow")
# ═══════════════════════════════════════════════════════════════
AIRFLOW_WEB_PID=""
AIRFLOW_SCHED_PID=""
DISPATCH_PID=""
RECONCILE_PID=""

# We now start Airflow and its workers regardless of the active engine.
# This ensures that zero-downtime migrations TO Airflow are possible because
# the Airflow API needs to be reachable to receive the schedules.
    echo -e "${GREEN}🌬️  Bootstrapping Apache Airflow...${NC}"

    cd backend
    export AIRFLOW_HOME="$(pwd)/airflow_home"
    export AIRFLOW__CORE__DAGS_FOLDER="$AIRFLOW_HOME/dags"
    mkdir -p "$AIRFLOW_HOME/dags" "$AIRFLOW_HOME/logs"

    # ── 1. Initialize / migrate the Airflow metadata DB ─────────
    echo -e "   ${CYAN}Initializing Airflow metadata database...${NC}"
    uv run --extra airflow airflow db migrate 2>&1 | tail -1

    # ── 2. Ensure an admin user exists ──────────────────────────
    AIRFLOW_ADMIN_USER="${AIRFLOW_USERNAME:-airflow}"
    AIRFLOW_ADMIN_PASS="${AIRFLOW_PASSWORD:-airflow}"

    if ! uv run --extra airflow airflow users list 2>/dev/null | grep -q "$AIRFLOW_ADMIN_USER"; then
        echo -e "   ${CYAN}Creating Airflow admin user '${AIRFLOW_ADMIN_USER}'...${NC}"
        uv run --extra airflow airflow users create \
            --username "$AIRFLOW_ADMIN_USER" \
            --password "$AIRFLOW_ADMIN_PASS" \
            --firstname Admin \
            --lastname User \
            --role Admin \
            --email admin@klex.local 2>&1 | tail -1
    else
        echo -e "   ${CYAN}Airflow admin user '${AIRFLOW_ADMIN_USER}' already exists.${NC}"
    fi

    # ── 3. Start the Airflow scheduler ──────────────────────────
    echo -e "${GREEN}🌬️  Starting Airflow scheduler...${NC}"
    uv run --extra airflow airflow scheduler \
        --log-file "$AIRFLOW_HOME/logs/scheduler.log" &
    AIRFLOW_SCHED_PID=$!

    # ── 4. Start the Airflow dag-processor (required for v3) ────────
    echo -e "${GREEN}🌬️  Starting Airflow DAG processor...${NC}"
    uv run --extra airflow airflow dag-processor \
        --log-file "$AIRFLOW_HOME/logs/dag-processor.log" &
    AIRFLOW_DAGPROC_PID=$!

    # ── 5. Start the Airflow webserver (API server in v3) ─────────────────
    AIRFLOW_PORT="${AIRFLOW_WEBSERVER_PORT:-8080}"
    echo -e "${GREEN}🌬️  Starting Airflow api-server on http://localhost:${AIRFLOW_PORT}${NC}"
    uv run --extra airflow airflow api-server \
        --port "$AIRFLOW_PORT" \
        --log-file "$AIRFLOW_HOME/logs/webserver.log" &
    AIRFLOW_WEB_PID=$!

    cd ..

    # ── 5. Wait for Airflow to become healthy, then start workers
    echo -e "   ${CYAN}Waiting for Airflow webserver to become ready...${NC}"
    AIRFLOW_READY=false
    for i in $(seq 1 30); do
        if curl -sf "http://localhost:${AIRFLOW_PORT}/" > /dev/null 2>&1; then
            AIRFLOW_READY=true
            break
        fi
        sleep 2
    done

    if [ "$AIRFLOW_READY" = true ]; then
        echo -e "   ${GREEN}✅ Airflow is healthy.${NC}"
    else
        echo -e "   ${YELLOW}⚠️  Airflow health check timed out. Dispatch workers will start anyway.${NC}"
    fi

    # ── 6. Start the dispatch loop (sends WAITING jobs → Airflow)
    echo -e "${GREEN}📤 Starting dispatch worker (every 15s)...${NC}"
    (
        cd backend
        while true; do
            uv run python manage.py dispatch_jobs 2>&1 | grep -v "^$" || true
            sleep 15
        done
    ) &
    DISPATCH_PID=$!

    # ── 7. Start the reconciliation loop (syncs Airflow → app DB)
    echo -e "${GREEN}🔄 Starting reconciliation worker (every 30s)...${NC}"
    (
        cd backend
        while true; do
            uv run python manage.py reconcile_airflow --retry 2>&1 | grep -v "^$" || true
            sleep 30
        done
    ) &
    RECONCILE_PID=$!
# End of Airflow block

# ═══════════════════════════════════════════════════════════════
#  Ready!
# ═══════════════════════════════════════════════════════════════
echo ""
echo -e "${GREEN}✅ Klex is running!${NC}"
echo -e "   Frontend:          http://localhost:5173"
echo -e "   Django Backend:    http://localhost:8000"
echo -e "   Reporting Service: http://localhost:8081"
if [ "$ACTIVE_ENGINE" = "airflow" ]; then
    echo -e "   Airflow UI:        http://localhost:${AIRFLOW_PORT:-8080}"
    echo -e "   Airflow Login:     ${AIRFLOW_ADMIN_USER:-airflow} / ${AIRFLOW_ADMIN_PASS:-airflow}"
    echo -e "   Dispatch Worker:   running (every 15s)"
    echo -e "   Reconcile Worker:  running (every 30s)"
fi
echo -e "   Admin Panel:       http://localhost:8000/admin/"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop all services.${NC}"

# Wait for background processes
wait $BACKEND_PID $FRONTEND_PID $REPORTING_PID $AIRFLOW_WEB_PID $AIRFLOW_SCHED_PID $DISPATCH_PID $RECONCILE_PID 2>/dev/null
