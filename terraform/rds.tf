# ═══════════════════════════════════════════════════════════════
#  RDS — PostgreSQL 15
# ═══════════════════════════════════════════════════════════════

resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = { Name = "${var.project_name}-db-subnet-group" }
}

resource "aws_db_instance" "main" {
  identifier = "${var.project_name}-${var.environment}-db"

  # Engine
  engine               = "postgres"
  engine_version       = "15"
  instance_class       = var.db_instance_class
  allocated_storage    = var.db_allocated_storage
  max_allocated_storage = var.db_allocated_storage * 2  # Autoscaling ceiling

  # Database
  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  # Networking
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false
  multi_az               = false  # Set to true for HA in production

  # Storage
  storage_type      = "gp3"
  storage_encrypted = true

  # Maintenance
  backup_retention_period = 7
  skip_final_snapshot     = true  # Set to false for production
  deletion_protection     = false # Set to true for production

  # Performance Insights (free tier for db.t3.micro)
  performance_insights_enabled = true

  tags = { Name = "${var.project_name}-db" }
}
