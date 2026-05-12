# ═══════════════════════════════════════════════════════════════
#  Klex — Input Variables
# ═══════════════════════════════════════════════════════════════

# ── General ───────────────────────────────────────────────────

variable "project_name" {
  description = "Name prefix for all resources"
  type        = string
  default     = "klex"
}

variable "environment" {
  description = "Deployment environment (e.g. production, staging)"
  type        = string
  default     = "production"
}

variable "aws_region" {
  description = "AWS region to deploy to"
  type        = string
  default     = "us-east-1"
}

# ── Networking ────────────────────────────────────────────────

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

# ── Database (RDS) ────────────────────────────────────────────

variable "db_instance_class" {
  description = "RDS instance type"
  type        = string
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  description = "Allocated storage in GB for RDS"
  type        = number
  default     = 20
}

variable "db_name" {
  description = "Name of the PostgreSQL database"
  type        = string
  default     = "klex"
}

variable "db_username" {
  description = "Master username for the database"
  type        = string
  default     = "klex_admin"
}

variable "db_password" {
  description = "Master password for the database (min 8 characters)"
  type        = string
  sensitive   = true
}

# ── ECS (Fargate) ─────────────────────────────────────────────

variable "backend_cpu" {
  description = "CPU units for the backend task (1024 = 1 vCPU)"
  type        = number
  default     = 1024
}

variable "backend_memory" {
  description = "Memory in MiB for the backend task"
  type        = number
  default     = 2048
}

variable "frontend_cpu" {
  description = "CPU units for the frontend task"
  type        = number
  default     = 256
}

variable "frontend_memory" {
  description = "Memory in MiB for the frontend task"
  type        = number
  default     = 512
}

variable "backend_desired_count" {
  description = "Number of backend task instances"
  type        = number
  default     = 1
}

variable "frontend_desired_count" {
  description = "Number of frontend task instances"
  type        = number
  default     = 1
}

# ── Application Secrets ───────────────────────────────────────

variable "django_secret_key" {
  description = "Django SECRET_KEY for cryptographic signing"
  type        = string
  sensitive   = true
}

variable "github_client_id" {
  description = "GitHub OAuth App client ID"
  type        = string
  default     = ""
}

variable "github_client_secret" {
  description = "GitHub OAuth App client secret"
  type        = string
  sensitive   = true
  default     = ""
}

variable "smtp_host" {
  description = "SMTP server hostname for email delivery"
  type        = string
  default     = "smtp.gmail.com"
}

variable "smtp_port" {
  description = "SMTP server port"
  type        = number
  default     = 587
}

variable "smtp_username" {
  description = "SMTP authentication username"
  type        = string
  default     = ""
}

variable "smtp_password" {
  description = "SMTP authentication password"
  type        = string
  sensitive   = true
  default     = ""
}

# ── DNS / SSL (Optional) ─────────────────────────────────────
# Set domain_name to enable Route 53 + ACM SSL certificate.
# Leave empty to use the raw ALB URL.

variable "domain_name" {
  description = "Custom domain name (e.g. klex.example.com). Leave empty to skip DNS/SSL setup."
  type        = string
  default     = ""
}

variable "route53_zone_id" {
  description = "Route 53 hosted zone ID for the domain. Required when domain_name is set."
  type        = string
  default     = ""
}
