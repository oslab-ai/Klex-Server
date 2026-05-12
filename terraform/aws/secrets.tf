# ═══════════════════════════════════════════════════════════════
#  AWS Secrets Manager — Application Secrets
# ═══════════════════════════════════════════════════════════════

resource "aws_secretsmanager_secret" "klex_app" {
  name                    = "${var.project_name}-${var.environment}-app-secrets"
  description             = "Klex application secrets (Django, GitHub OAuth, SMTP)"
  recovery_window_in_days = 7

  tags = { Name = "${var.project_name}-app-secrets" }
}

resource "aws_secretsmanager_secret_version" "klex_app" {
  secret_id = aws_secretsmanager_secret.klex_app.id

  secret_string = jsonencode({
    DJANGO_SECRET_KEY    = var.django_secret_key
    GITHUB_CLIENT_ID     = var.github_client_id
    GITHUB_CLIENT_SECRET = var.github_client_secret
    SPRING_MAIL_HOST     = var.smtp_host
    SPRING_MAIL_PORT     = tostring(var.smtp_port)
    SPRING_MAIL_USERNAME = var.smtp_username
    SPRING_MAIL_PASSWORD = var.smtp_password
    DATABASE_URL         = "postgres://${var.db_username}:${var.db_password}@${aws_db_instance.main.endpoint}/${var.db_name}"
  })
}
