# ═══════════════════════════════════════════════════════════════
#  Outputs
# ═══════════════════════════════════════════════════════════════

output "aws_region" {
  description = "AWS region used for deployment"
  value       = var.aws_region
}

# ── Application URLs ──────────────────────────────────────────

output "app_url" {
  description = "URL to access Klex"
  value       = var.domain_name != "" ? "https://${var.domain_name}" : "http://${aws_lb.main.dns_name}"
}

output "alb_dns_name" {
  description = "ALB DNS name (raw)"
  value       = aws_lb.main.dns_name
}

# ── Database ──────────────────────────────────────────────────

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = aws_db_instance.main.endpoint
}

output "rds_database_name" {
  description = "RDS database name"
  value       = aws_db_instance.main.db_name
}

# ── ECR Repositories ─────────────────────────────────────────

output "ecr_backend_url" {
  description = "ECR repository URL for the backend image"
  value       = aws_ecr_repository.backend.repository_url
}

output "ecr_frontend_url" {
  description = "ECR repository URL for the frontend image"
  value       = aws_ecr_repository.frontend.repository_url
}

# ── ECS ───────────────────────────────────────────────────────

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "ecs_backend_service" {
  description = "ECS backend service name"
  value       = aws_ecs_service.backend.name
}

output "ecs_frontend_service" {
  description = "ECS frontend service name"
  value       = aws_ecs_service.frontend.name
}

# ── Secrets ───────────────────────────────────────────────────

output "secrets_manager_arn" {
  description = "ARN of the Secrets Manager secret"
  value       = aws_secretsmanager_secret.klex_app.arn
}

# ── Networking ────────────────────────────────────────────────

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

# ── Quick Start Commands ──────────────────────────────────────

output "next_steps" {
  description = "Commands to deploy your application"
  value       = <<-EOT

    ✅ Infrastructure is ready! Next steps:

    1. Build and push Docker images:
       ./deploy.sh

    2. Open your app:
       ${var.domain_name != "" ? "https://${var.domain_name}" : "http://${aws_lb.main.dns_name}"}

    3. View logs:
       aws logs tail /ecs/${var.project_name}/backend --follow
       aws logs tail /ecs/${var.project_name}/frontend --follow

  EOT
}
