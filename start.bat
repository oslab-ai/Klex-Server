@echo off
REM ═══════════════════════════════════════════════════════════════
REM  Klex Startup Script (Windows)
REM  Runs: Django backend, Vite frontend, Java Reporting Service
REM  Note: Apache Airflow is not supported on Windows natively.
REM        Use WSL2 or Docker for Airflow scheduling.
REM ═══════════════════════════════════════════════════════════════
setlocal enabledelayedexpansion

echo.
echo  ======================================
echo   Klex Development Environment
echo  ======================================
echo.

REM ── Check for .env file ─────────────────────────────────────
if not exist ".env" (
    if exist ".env.example" (
        echo [WARN] No .env file found. Copying from .env.example...
        copy .env.example .env >nul
        echo [WARN] Please edit .env with your configuration.
        echo [WARN] Required: GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, DJANGO_SECRET_KEY
    ) else (
        echo [ERROR] No .env or .env.example found. Please create a .env file.
        exit /b 1
    )
)

REM ── Load .env into environment ──────────────────────────────
for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
    set "line=%%a"
    if not "!line:~0,1!"=="#" (
        if not "%%a"=="" (
            set "%%a=%%b"
        )
    )
)

REM ── Auto-derive Spring datasource from DATABASE_URL ─────────
if defined DATABASE_URL (
    if not defined SPRING_DATASOURCE_URL (
        REM Parse DATABASE_URL: postgres://user:pass@host:port/db
        set "DB_URL=%DATABASE_URL%"
        set "DB_URL=!DB_URL:postgres://=!"

        for /f "tokens=1,2 delims=@" %%x in ("!DB_URL!") do (
            set "DB_USERPASS=%%x"
            set "DB_HOSTDB=%%y"
        )
        for /f "tokens=1,2 delims=:" %%x in ("!DB_USERPASS!") do (
            set "SPRING_DATASOURCE_USERNAME=%%x"
            set "SPRING_DATASOURCE_PASSWORD=%%y"
        )
        set "SPRING_DATASOURCE_URL=jdbc:postgresql://!DB_HOSTDB!"
        echo [INFO] Auto-derived Spring datasource from DATABASE_URL
    )
)

REM ── Check prerequisites ─────────────────────────────────────
where uv >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] uv is not installed.
    echo         Install: https://docs.astral.sh/uv/getting-started/installation/
    exit /b 1
)

where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] npm is not installed. Please install Node.js.
    exit /b 1
)

set MAVEN_AVAILABLE=false
where mvn >nul 2>&1
if %errorlevel% equ 0 (
    set MAVEN_AVAILABLE=true
) else (
    echo [WARN] Maven is not installed. Java reporting service will not start.
)

REM ── Backend setup ───────────────────────────────────────────
echo [INFO] Setting up backend...
cd backend

echo        Installing Python dependencies with uv...
uv sync

echo        Running database migrations...
uv run python manage.py migrate

if not exist "media" mkdir media

echo [INFO] Starting Django backend on http://localhost:8000
start "Django Backend" /b cmd /c "uv run python manage.py runserver 0.0.0.0:8000"

cd ..

REM ── Frontend setup ──────────────────────────────────────────
echo [INFO] Setting up frontend...
cd frontend

echo        Installing npm dependencies...
call npm install

echo [INFO] Starting Vite frontend on http://localhost:5173
start "Vite Frontend" /b cmd /c "npm run dev"

cd ..

REM ── Java Reporting Service ──────────────────────────────────
if "%MAVEN_AVAILABLE%"=="true" (
    if exist "services\reporting-service\pom.xml" (
        echo [INFO] Starting Reporting Service on http://localhost:8081
        cd services\reporting-service
        start "Reporting Service" /b cmd /c "mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081 -q"
        cd ..\..
    ) else (
        echo [WARN] No reporting service found at services\reporting-service. Skipping.
    )
)

REM ── Ready! ──────────────────────────────────────────────────
echo.
echo  ======================================
echo   Klex is running!
echo  ======================================
echo   Frontend:          http://localhost:5173
echo   Django Backend:    http://localhost:8000
echo   Reporting Service: http://localhost:8081
echo   Admin Panel:       http://localhost:8000/admin/
echo.
echo   Note: Apache Airflow is not supported on Windows.
echo         Use WSL2 or Docker for Airflow scheduling.
echo.
echo   Press Ctrl+C in each window to stop services.
echo.

REM Keep this window open
pause
