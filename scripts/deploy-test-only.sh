#!/bin/bash

# Deploy to Test Environment Only
# This script deploys only to test environment for validation

set -e

echo "🧪 Deploying to TEST Environment Only..."

# Build the application
echo "📦 Building application..."
./gradlew clean build -x test

# Build Docker image
echo "🐳 Building Docker image..."
docker build -t user-management-service:test-$(date +%s) .

# You can add AWS deployment commands here for test environment only
echo "🚀 Ready to deploy to test environment"
echo ""
echo "Next steps:"
echo "1. Push image to ECR"
echo "2. Update ECS test service"
echo "3. Run health checks"
echo "4. Validate database connectivity"
echo ""
echo "⚠️  This script does NOT deploy to production"
