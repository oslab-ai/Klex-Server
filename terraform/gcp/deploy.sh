#!/bin/bash

set -e

PROJECT_ID="your-project-id"
REGION="us-central1"
REPOSITORY="klex"

BACKEND_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$REPOSITORY/backend:latest"
FRONTEND_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$REPOSITORY/frontend:latest"

# Configure Docker auth
gcloud auth configure-docker $REGION-docker.pkg.dev

# Build images
docker build -t $BACKEND_IMAGE ..
docker build -f ../Dockerfile.frontend.prod -t $FRONTEND_IMAGE ..

# Push images
docker push $BACKEND_IMAGE
docker push $FRONTEND_IMAGE

# Redeploy services
gcloud run deploy klex-backend \
  --image $BACKEND_IMAGE \
  --region $REGION

gcloud run deploy klex-frontend \
  --image $FRONTEND_IMAGE \
  --region $REGION
