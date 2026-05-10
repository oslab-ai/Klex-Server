FROM python:3.11-slim-bookworm

# Install Java, Maven, supervisor, and build deps
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jdk-headless \
    maven \
    supervisor \
    gcc \
    libpq-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# ─────────────────────────────
# Build Java Reporting Service
# ─────────────────────────────
WORKDIR /build/java

# Copy Maven config first for dependency caching
COPY services/reporting-service/pom.xml .
RUN mvn dependency:go-offline -B

# Copy Klex reports library (build dependency)
# COPY services/reporting-service/klex reports-master ./klex reports-master

# Copy Java source and build
COPY services/reporting-service/src ./src
RUN mvn package -DskipTests -q

# ─────────────────────────────
# Setup Django
# ─────────────────────────────
WORKDIR /app

# pyproject.toml and uv.lock are at root
COPY pyproject.toml uv.lock* ./

RUN pip install --no-cache-dir uv && uv sync --no-dev --no-cache --no-install-project && uv pip install gunicorn

# Copy Django app into /app/backend
COPY backend/ ./backend/

WORKDIR /app/backend
RUN cd /app && uv run python backend/manage.py collectstatic --noinput || true

# ─────────────────────────────
# Move built Java jar
# ─────────────────────────────
RUN mkdir -p /app/java
RUN cp /build/java/target/*.jar /app/java/app.jar && rm -rf /build

# ─────────────────────────────
# Supervisor
# ─────────────────────────────
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# ─────────────────────────────
# Entrypoint
# ─────────────────────────────
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8000 8081

ENTRYPOINT ["/entrypoint.sh"]

CMD ["supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]