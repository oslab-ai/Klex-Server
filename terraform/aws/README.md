# Klex — AWS Terraform Deployment

Deploy the full Klex stack (Django + Java backend, React frontend, PostgreSQL) to AWS using **ECS Fargate** — no servers to manage.

## What Gets Created

| AWS Resource | Purpose | Estimated Monthly Cost |
|-------------|---------|----------------------|
| **ECS Fargate** (2 tasks) | Runs backend + frontend containers | ~$30–50 |
| **RDS PostgreSQL** (db.t3.micro) | Managed database | ~$15 |
| **ALB** | Load balancer + routing | ~$18 |
| **NAT Gateway** | Outbound internet for private subnets | ~$32 |
| **ECR** (2 repos) | Container image storage | ~$1 |
| **Secrets Manager** | Secure secret storage | ~$0.40 |
| **CloudWatch Logs** | Centralized logging | ~$1 |
| **Route 53 + ACM** *(optional)* | Custom domain + SSL | ~$0.50 |

> **Total estimate: ~$95–120/month** for a minimal single-instance deployment.

## Architecture

```
                Internet
                   │
            ┌──────┴──────┐
            │     ALB     │  (HTTP/HTTPS)
            └──────┬──────┘
         ┌─────────┼─────────┐
         │ /api/*  │  /*     │
         ▼         │         ▼
    ┌─────────┐    │   ┌──────────┐
    │ Backend │    │   │ Frontend │
    │ (ECS)   │    │   │  (ECS)   │
    │ Django  │    │   │  Nginx   │
    │ + Java  │    │   │          │
    └────┬────┘    │   └──────────┘
         │         │
    ┌────┴────┐    │
    │   RDS   │    │
    │ Postgres│    │
    └─────────┘    │
                   │
         All in private subnets
         behind NAT Gateway
```

## Prerequisites

1. **AWS CLI** configured with credentials:
   ```bash
   aws configure
   ```
   > **Note:** You must use **Programmatic Access Keys** (Access Key ID and Secret Access Key) from the IAM console, not your AWS Console login credentials.

2. **Terraform** ≥ 1.10:
   ```bash
   # macOS
   brew install terraform

   # Ubuntu / Debian
   curl -fsSL https://apt.releases.hashicorp.com/gpg | sudo apt-key add -
   sudo apt-add-repository "deb https://apt.releases.hashicorp.com $(lsb_release -cs) main"
   sudo apt update && sudo apt install terraform
   ```

3. **Docker** (for building images):
   ```bash
   docker --version  # Must be installed and running
   ```

## Quick Start

### Step 1 — Configure Variables

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars` with your values. **At minimum, change these:**

```hcl
db_password       = "your-strong-database-password"
django_secret_key = "your-random-secret-key"
```

> **Tip:** Generate a Django secret key:
> ```bash
> python3 -c "import secrets; print(secrets.token_urlsafe(50))"
> ```

### Step 2 — Deploy Infrastructure

```bash
terraform init      # Download providers
terraform plan      # Preview what will be created
terraform apply     # Create everything (type 'yes' to confirm)
```

This takes **5–10 minutes** (RDS creation is the bottleneck).

### Step 3 — Build & Push Docker Images

```bash
chmod +x deploy.sh
./deploy.sh
```

This script:
1. Reads ECR URLs from Terraform outputs
2. Builds the backend image (from `Dockerfile`)
3. Builds the frontend image (from `Dockerfile.frontend.prod`)
4. Pushes both to ECR
5. Triggers ECS to pull the new images

### Step 4 — Access Your App

```bash
terraform output app_url
```

Open the URL in your browser. The first startup takes **2–3 minutes** as ECS pulls images and runs migrations.

## Custom Domain + SSL (Optional)

To use a custom domain with HTTPS:

### 1. Create a Route 53 Hosted Zone

If you don't already have one:
```bash
aws route53 create-hosted-zone --name example.com --caller-reference $(date +%s)
```

Note the **Hosted Zone ID** from the output.

### 2. Update Your Domain's NS Records

Point your domain registrar's nameservers to the Route 53 NS records shown in the hosted zone.

### 3. Add to terraform.tfvars

```hcl
domain_name     = "klex.example.com"
route53_zone_id = "Z0123456789ABCDEFGHIJ"
```

### 4. Apply

```bash
terraform apply
```

Terraform will:
- Create an ACM SSL certificate
- Validate it via DNS (automatic with Route 53)
- Configure HTTPS on the ALB
- Redirect HTTP → HTTPS
- Create an A record pointing to the ALB

## Remote State (Optional)

For team usage, store Terraform state in S3:

### 1. Create an S3 Bucket

```bash
aws s3api create-bucket \
    --bucket klex-terraform-state \
    --region us-east-1

aws s3api put-bucket-versioning \
    --bucket klex-terraform-state \
    --versioning-configuration Status=Enabled
```

### 2. Uncomment the Backend Block

In `main.tf`, uncomment the `backend "s3"` block and update the bucket name:

```hcl
backend "s3" {
  bucket       = "klex-terraform-state"
  key          = "klex/terraform.tfstate"
  region       = "us-east-1"
  encrypt      = true
  use_lockfile = true
}
```

### 3. Migrate State

```bash
terraform init -migrate-state
```

## Common Operations

### Redeploy After Code Changes

```bash
cd terraform
./deploy.sh
```

### View Logs

```bash
# Backend logs (Django + Java)
aws logs tail /ecs/klex/backend --follow --region us-east-1

# Frontend logs (Nginx)
aws logs tail /ecs/klex/frontend --follow --region us-east-1
```

### Scale Up

Edit `terraform.tfvars`:
```hcl
backend_desired_count  = 2
frontend_desired_count = 2
```

```bash
terraform apply
```

### Upgrade RDS Instance

```hcl
db_instance_class = "db.t3.small"
```

```bash
terraform apply
```

### SSH / Exec into a Container

```bash
# Enable ECS Exec (one-time)
aws ecs update-service --cluster klex-cluster \
    --service klex-backend \
    --enable-execute-command

# Connect
aws ecs execute-command --cluster klex-cluster \
    --task <TASK_ID> \
    --container backend \
    --interactive \
    --command "/bin/bash"
```

### Run Django Management Commands

```bash
aws ecs execute-command --cluster klex-cluster \
    --task <TASK_ID> \
    --container backend \
    --interactive \
    --command "/app/.venv/bin/python /app/backend/manage.py migrate"
```

## Tear Down

```bash
terraform destroy   # Type 'yes' to confirm
```

> **Warning:** This deletes everything including the database. Make sure to back up your data first.

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `terraform apply` fails at RDS | DB password might be too short. Use ≥ 8 characters. |
| ECS tasks keep restarting | Check CloudWatch logs: `aws logs tail /ecs/klex/backend --follow` |
| ALB returns 503 | Tasks haven't started yet. Wait 2–3 min, then check ECS console. |
| Health check failing | Backend needs ~60s to start (Java + Django). Check `startPeriod` in task def. |
| ECR push denied | Re-run `aws ecr get-login-password` or check IAM permissions. |
| ACM cert stuck "Pending" | DNS validation records may not have propagated. Check Route 53. |
| NAT Gateway costs too high | For dev/test, you can modify `vpc.tf` to use public subnets + assign public IPs to ECS tasks instead. |

## File Reference

| File | Purpose |
|------|---------|
| `main.tf` | Provider + S3 backend configuration |
| `variables.tf` | All input variables with defaults |
| `vpc.tf` | VPC, subnets, NAT gateway, security groups |
| `iam.tf` | IAM roles for ECS |
| `ecr.tf` | Container registries |
| `rds.tf` | PostgreSQL database |
| `alb.tf` | Load balancer + routing rules |
| `ecs.tf` | Fargate cluster, task definitions, services |
| `dns.tf` | Route 53 + ACM (conditional) |
| `secrets.tf` | Secrets Manager |
| `outputs.tf` | Output values |
| `deploy.sh` | Build images + push to ECR + trigger deploy |
| `terraform.tfvars.example` | Template for your variables |
