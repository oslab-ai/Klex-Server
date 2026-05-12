# ═══════════════════════════════════════════════════════════════
#  Klex — Terraform AWS Deployment
#  Provider & Backend Configuration
# ═══════════════════════════════════════════════════════════════

terraform {
  required_version = ">= 1.10.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # ── S3 Remote State (with S3-native locking) ──────────────────
  # Uncomment after creating the S3 bucket:
  #   aws s3api create-bucket --bucket <your-bucket-name> --region <region>
  #   aws s3api put-bucket-versioning --bucket <your-bucket-name> \
  #       --versioning-configuration Status=Enabled
  #
  # backend "s3" {
  #   bucket       = "klex-terraform-state"
  #   key          = "klex/terraform.tfstate"
  #   region       = "us-east-1"
  #   encrypt      = true
  #   use_lockfile = true   # S3-native state locking (no DynamoDB needed)
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# Data sources
data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}
