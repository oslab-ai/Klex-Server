# ═══════════════════════════════════════════════════════════════
#  ECS Fargate — Cluster, Task Definitions, Services
# ═══════════════════════════════════════════════════════════════

# ── CloudWatch Log Groups ─────────────────────────────────────

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/${var.project_name}/backend"
  retention_in_days = 30
  tags              = { Name = "${var.project_name}-backend-logs" }
}

resource "aws_cloudwatch_log_group" "frontend" {
  name              = "/ecs/${var.project_name}/frontend"
  retention_in_days = 30
  tags              = { Name = "${var.project_name}-frontend-logs" }
}

# ── ECS Cluster ───────────────────────────────────────────────

resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = { Name = "${var.project_name}-cluster" }
}

# ═══════════════════════════════════════════════════════════════
#  Backend Task Definition
#  (Django + Java Reporting Service in one container)
# ═══════════════════════════════════════════════════════════════

locals {
  # Build the ALB URL for FRONTEND_BASE_URL / BACKEND_BASE_URL
  alb_url    = var.domain_name != "" ? "https://${var.domain_name}" : "http://${aws_lb.main.dns_name}"
  db_host    = split(":", aws_db_instance.main.endpoint)[0]
  db_port    = "5432"
  spring_url = "jdbc:postgresql://${local.db_host}:${local.db_port}/${var.db_name}"
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "${var.project_name}-backend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.backend_cpu
  memory                   = var.backend_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name      = "backend"
    image     = "${aws_ecr_repository.backend.repository_url}:latest"
    essential = true

    portMappings = [
      { containerPort = 8000, protocol = "tcp" },
      { containerPort = 8081, protocol = "tcp" },
    ]

    environment = [
      { name = "DJANGO_DEBUG", value = "False" },
      { name = "DJANGO_ALLOWED_HOSTS", value = var.domain_name != "" ? var.domain_name : "*" },
      { name = "FRONTEND_BASE_URL", value = local.alb_url },
      { name = "BACKEND_BASE_URL", value = local.alb_url },
      { name = "COMPILER_SERVICE_URL", value = "http://localhost:8081" },
      { name = "SCHEDULER_SERVICE_URL", value = "http://localhost:8081" },
      { name = "SPRING_DATASOURCE_URL", value = local.spring_url },
      { name = "SPRING_DATASOURCE_USERNAME", value = var.db_username },
      { name = "SPRING_DATASOURCE_PASSWORD", value = var.db_password },
    ]

    secrets = [
      { name = "DJANGO_SECRET_KEY", valueFrom = "${aws_secretsmanager_secret.klex_app.arn}:DJANGO_SECRET_KEY::" },
      { name = "GITHUB_CLIENT_ID", valueFrom = "${aws_secretsmanager_secret.klex_app.arn}:GITHUB_CLIENT_ID::" },
      { name = "GITHUB_CLIENT_SECRET", valueFrom = "${aws_secretsmanager_secret.klex_app.arn}:GITHUB_CLIENT_SECRET::" },
      { name = "DATABASE_URL", valueFrom = "${aws_secretsmanager_secret.klex_app.arn}:DATABASE_URL::" },
      { name = "SPRING_MAIL_HOST", valueFrom = "${aws_secretsmanager_secret.klex_app.arn}:SPRING_MAIL_HOST::" },
      { name = "SPRING_MAIL_PORT", valueFrom = "${aws_secretsmanager_secret.klex_app.arn}:SPRING_MAIL_PORT::" },
      { name = "SPRING_MAIL_USERNAME", valueFrom = "${aws_secretsmanager_secret.klex_app.arn}:SPRING_MAIL_USERNAME::" },
      { name = "SPRING_MAIL_PASSWORD", valueFrom = "${aws_secretsmanager_secret.klex_app.arn}:SPRING_MAIL_PASSWORD::" },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.backend.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "backend"
      }
    }

    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:8000/api/health/ || exit 1"]
      interval    = 30
      timeout     = 10
      retries     = 3
      startPeriod = 60
    }
  }])

  tags = { Name = "${var.project_name}-backend-task" }
}

# ═══════════════════════════════════════════════════════════════
#  Frontend Task Definition
#  (Nginx serving the production React build)
# ═══════════════════════════════════════════════════════════════

resource "aws_ecs_task_definition" "frontend" {
  family                   = "${var.project_name}-frontend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.frontend_cpu
  memory                   = var.frontend_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name      = "frontend"
    image     = "${aws_ecr_repository.frontend.repository_url}:latest"
    essential = true

    portMappings = [
      { containerPort = 80, protocol = "tcp" },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.frontend.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "frontend"
      }
    }

    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost/ || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 30
    }
  }])

  tags = { Name = "${var.project_name}-frontend-task" }
}

# ═══════════════════════════════════════════════════════════════
#  ECS Services
# ═══════════════════════════════════════════════════════════════

resource "aws_ecs_service" "backend" {
  name            = "${var.project_name}-backend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.backend_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = 8000
  }

  depends_on = [
    aws_lb_listener.http,
    aws_iam_role_policy_attachment.ecs_execution_base,
  ]

  tags = { Name = "${var.project_name}-backend-service" }
}

resource "aws_ecs_service" "frontend" {
  name            = "${var.project_name}-frontend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.frontend.arn
  desired_count   = var.frontend_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.frontend.arn
    container_name   = "frontend"
    container_port   = 80
  }

  depends_on = [
    aws_lb_listener.http,
    aws_iam_role_policy_attachment.ecs_execution_base,
  ]

  tags = { Name = "${var.project_name}-frontend-service" }
}
