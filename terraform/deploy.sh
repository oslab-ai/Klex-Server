#!/bin/bash

set -e

RESOURCE_GROUP="klex-rg"
REGISTRY_NAME="klexacr123"

BACKEND_IMAGE="$REGISTRY_NAME.azurecr.io/backend:latest"
FRONTEND_IMAGE="$REGISTRY_NAME.azurecr.io/frontend:latest"

# Login to Azure
az login

# Login to ACR
az acr login --name $REGISTRY_NAME

# Build images
docker build -t $BACKEND_IMAGE ..
docker build -f ../Dockerfile.frontend.prod -t $FRONTEND_IMAGE ..

# Push images
docker push $BACKEND_IMAGE
docker push $FRONTEND_IMAGE

# Update container apps
az containerapp update \
  --name klex-backend \
  --resource-group $RESOURCE_GROUP \
  --image $BACKEND_IMAGE

az containerapp update \
  --name klex-frontend \
  --resource-group $RESOURCE_GROUP \
  --image $FRONTEND_IMAGE
