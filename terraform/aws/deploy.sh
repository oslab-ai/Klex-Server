#!/bin/bash
# ═══════════════════════════════════════════════════════════════
#  Klex — Build & Deploy to AWS ECS
#  This script builds Docker images, pushes to ECR,
#  and triggers a new ECS deployment.
# ═══════════════════════════════════════════════════════════════

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# ── Get Terraform outputs ────────────────────────────────────
echo -e "${CYAN}📦 Reading Terraform outputs...${NC}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

AWS_REGION=$(terraform output -raw aws_region 2>/dev/null || echo "us-east-1")
ECR_BACKEND=$(terraform output -raw ecr_backend_url 2>/dev/null)
ECR_FRONTEND=$(terraform output -raw ecr_frontend_url 2>/dev/null)
ECS_CLUSTER=$(terraform output -raw ecs_cluster_name 2>/dev/null)
ECS_BACKEND_SERVICE=$(terraform output -raw ecs_backend_service 2>/dev/null)
ECS_FRONTEND_SERVICE=$(terraform output -raw ecs_frontend_service 2>/dev/null)

if [ -z "$ECR_BACKEND" ] || [ -z "$ECR_FRONTEND" ]; then
    echo -e "${RED}❌ Could not read Terraform outputs. Run 'terraform apply' first.${NC}"
    exit 1
fi

AWS_ACCOUNT_ID=$(echo "$ECR_BACKEND" | cut -d'.' -f1)

echo -e "   Region:   ${GREEN}$AWS_REGION${NC}"
echo -e "   Backend:  ${GREEN}$ECR_BACKEND${NC}"
echo -e "   Frontend: ${GREEN}$ECR_FRONTEND${NC}"

# ── Authenticate to ECR ──────────────────────────────────────
echo -e "${CYAN}🔐 Logging in to ECR...${NC}"
aws ecr get-login-password --region "$AWS_REGION" \
    | docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"

# ── Build & Push Backend ──────────────────────────────────────
echo -e "${CYAN}🔧 Building backend image...${NC}"
cd "$SCRIPT_DIR/.."

docker build -t "${ECR_BACKEND}:latest" -f Dockerfile .
echo -e "${GREEN}⬆️  Pushing backend image...${NC}"
docker push "${ECR_BACKEND}:latest"

# ── Build & Push Frontend ─────────────────────────────────────
echo -e "${CYAN}⚛️  Building frontend image (production)...${NC}"
docker build -t "${ECR_FRONTEND}:latest" -f Dockerfile.frontend.prod .
echo -e "${GREEN}⬆️  Pushing frontend image...${NC}"
docker push "${ECR_FRONTEND}:latest"

# ── Force New ECS Deployment ──────────────────────────────────
echo -e "${CYAN}🚀 Triggering ECS deployment...${NC}"
aws ecs update-service \
    --cluster "$ECS_CLUSTER" \
    --service "$ECS_BACKEND_SERVICE" \
    --force-new-deployment \
    --region "$AWS_REGION" \
    --no-cli-pager > /dev/null

aws ecs update-service \
    --cluster "$ECS_CLUSTER" \
    --service "$ECS_FRONTEND_SERVICE" \
    --force-new-deployment \
    --region "$AWS_REGION" \
    --no-cli-pager > /dev/null

# ── Done ──────────────────────────────────────────────────────
APP_URL=$(terraform output -raw app_url 2>/dev/null || echo "check terraform output")

echo ""
echo -e "${GREEN}✅ Deployment triggered!${NC}"
echo -e "   ECS will pull new images and replace running tasks."
echo -e "   This typically takes 2-5 minutes."
echo ""
echo -e "   🌐 App URL: ${CYAN}${APP_URL}${NC}"
echo ""
echo -e "   Monitor progress:"
echo -e "     aws ecs describe-services --cluster $ECS_CLUSTER --services $ECS_BACKEND_SERVICE --query 'services[0].deployments' --region $AWS_REGION"
echo ""
