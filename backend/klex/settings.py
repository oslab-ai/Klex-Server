"""
Django settings for klex project.
"""
import os
from pathlib import Path
from datetime import timedelta

import dj_database_url
from dotenv import load_dotenv

# Load environment variables from root .env
BASE_DIR = Path(__file__).resolve().parent.parent
ROOT_DIR = BASE_DIR.parent
load_dotenv(ROOT_DIR / '.env', override=True)

SECRET_KEY = os.getenv('DJANGO_SECRET_KEY', 'django-insecure-dev-key-change-me')
DEBUG = os.getenv('DJANGO_DEBUG', 'False').lower() in ('true', '1', 'yes')

ALLOWED_HOSTS = os.getenv('DJANGO_ALLOWED_HOSTS', 'localhost,127.0.0.1,13.60.170.247').split(',')

# Application definition
INSTALLED_APPS = [
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    # Third party
    'rest_framework',
    'rest_framework_simplejwt',
    'corsheaders',
    # Local apps
    'accounts',
    'repos',
    'reports',
    'github_integration',
    'dataadapter',
    'audit',
    'chatbot',
]

MIDDLEWARE = [
    'corsheaders.middleware.CorsMiddleware',
    'django.middleware.security.SecurityMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',
]

ROOT_URLCONF = 'klex.urls'

TEMPLATES = [
    {
        'BACKEND': 'django.template.backends.django.DjangoTemplates',
        'DIRS': [],
        'APP_DIRS': True,
        'OPTIONS': {
            'context_processors': [
                'django.template.context_processors.debug',
                'django.template.context_processors.request',
                'django.contrib.auth.context_processors.auth',
                'django.contrib.messages.context_processors.messages',
            ],
        },
    },
]

WSGI_APPLICATION = 'klex.wsgi.application'

# Database
DATABASE_URL = os.getenv('DATABASE_URL', 'postgres://postgres:postgres@localhost:5432/klex')
DATABASES = {
    'default': dj_database_url.parse(DATABASE_URL)
}

# Custom User Model
AUTH_USER_MODEL = 'accounts.User'

# Password validation
AUTH_PASSWORD_VALIDATORS = [
    {'NAME': 'django.contrib.auth.password_validation.UserAttributeSimilarityValidator'},
    {'NAME': 'django.contrib.auth.password_validation.MinimumLengthValidator'},
    {'NAME': 'django.contrib.auth.password_validation.CommonPasswordValidator'},
    {'NAME': 'django.contrib.auth.password_validation.NumericPasswordValidator'},
]

# Internationalization
LANGUAGE_CODE = 'en-us'
TIME_ZONE = 'UTC'
USE_I18N = True
USE_TZ = True

# Static files
STATIC_URL = 'static/'
STATIC_ROOT = BASE_DIR / 'staticfiles'

# Media files (for sample PDFs, etc.)
MEDIA_URL = 'media/'
MEDIA_ROOT = BASE_DIR / 'media'

DEFAULT_AUTO_FIELD = 'django.db.models.BigAutoField'

# REST Framework
REST_FRAMEWORK = {
    'DEFAULT_AUTHENTICATION_CLASSES': [
        'rest_framework_simplejwt.authentication.JWTAuthentication',
    ],
    'DEFAULT_PERMISSION_CLASSES': [
        'rest_framework.permissions.IsAuthenticated',
    ],
}

# JWT Settings
SIMPLE_JWT = {
    'ACCESS_TOKEN_LIFETIME': timedelta(minutes=60),
    'REFRESH_TOKEN_LIFETIME': timedelta(days=7),
    'ROTATE_REFRESH_TOKENS': True,
    'BLACKLIST_AFTER_ROTATION': False,
    'AUTH_HEADER_TYPES': ('Bearer',),
}

# CORS Settings
FRONTEND_BASE_URL = os.getenv('FRONTEND_BASE_URL', 'http://localhost:5173')
CORS_ALLOWED_ORIGINS = [
    FRONTEND_BASE_URL,
]
CORS_ALLOW_CREDENTIALS = True

# GitHub OAuth settings
GITHUB_CLIENT_ID = os.getenv('GITHUB_CLIENT_ID', '')
GITHUB_CLIENT_SECRET = os.getenv('GITHUB_CLIENT_SECRET', '')
BACKEND_BASE_URL = os.getenv('BACKEND_BASE_URL', 'http://localhost:8000')

# Java Compiler & Scheduler Service (KlexReportingService)
COMPILER_SERVICE_URL = os.getenv('COMPILER_SERVICE_URL', 'http://localhost:8081')
SCHEDULER_SERVICE_URL = os.getenv('SCHEDULER_SERVICE_URL', COMPILER_SERVICE_URL)

# Apache Airflow — only used when scheduler_config.yaml active_engine = "airflow"
AIRFLOW_API_URL = os.getenv('AIRFLOW_API_URL', 'http://localhost:8080/api/v1')
AIRFLOW_USERNAME = os.getenv('AIRFLOW_USERNAME', 'airflow')
AIRFLOW_PASSWORD = os.getenv('AIRFLOW_PASSWORD', 'airflow')
AIRFLOW_TIMEOUT = int(os.getenv('AIRFLOW_TIMEOUT', '30'))
AIRFLOW_MAX_RETRIES = int(os.getenv('AIRFLOW_MAX_RETRIES', '3'))
AIRFLOW_BACKOFF_FACTOR = float(os.getenv('AIRFLOW_BACKOFF_FACTOR', '0.5'))

# Report Dispatch Configuration (Airflow-only)
DISPATCH_CONFIG = {
    'global_max_active_jobs': int(os.getenv('DISPATCH_GLOBAL_MAX_ACTIVE', '20')),
    'dispatch_batch_size': int(os.getenv('DISPATCH_BATCH_SIZE', '50')),
    'per_dag_max_active': int(os.getenv('DISPATCH_PER_DAG_MAX_ACTIVE', '3')),
    'per_tenant_max_active': int(os.getenv('DISPATCH_PER_TENANT_MAX_ACTIVE', '10')),
    'stale_job_timeout_seconds': int(os.getenv('DISPATCH_STALE_TIMEOUT', '3600')),
    'max_job_retries': int(os.getenv('DISPATCH_MAX_RETRIES', '3')),
    'min_retry_interval_seconds': int(os.getenv('DISPATCH_MIN_RETRY_INTERVAL', '60')),
}

# ── Email (reuse existing SPRING_MAIL_* credentials) ──────────
EMAIL_BACKEND = 'django.core.mail.backends.smtp.EmailBackend'
EMAIL_HOST = os.getenv('SPRING_MAIL_HOST', 'smtp.gmail.com')
EMAIL_PORT = int(os.getenv('SPRING_MAIL_PORT', '587'))
EMAIL_USE_TLS = True
EMAIL_HOST_USER = os.getenv('SPRING_MAIL_USERNAME', '')
EMAIL_HOST_PASSWORD = os.getenv('SPRING_MAIL_PASSWORD', '')
DEFAULT_FROM_EMAIL = os.getenv('SPRING_MAIL_USERNAME', 'noreply@klex.app')

# Comma-separated admin emails for failure notifications
FAILURE_NOTIFICATION_EMAIL = os.getenv(
    'FAILURE_NOTIFICATION_EMAIL',
    os.getenv('SPRING_MAIL_USERNAME', ''),
)
