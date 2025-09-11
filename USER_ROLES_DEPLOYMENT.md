# User-Roles Branch Deployment Guide

This guide explains how to deploy both test and production environments using the `user-roles` branch.

## 🚀 Quick Start

### 1. Push to user-roles Branch
```bash
# Make sure you're on the user-roles branch
git checkout user-roles

# Add your changes
git add .
git commit -m "Deploy user-roles to both environments"

# Push to trigger the pipeline
git push origin user-roles
```

### 2. What Happens Automatically

When you push to the `user-roles` branch, the CI/CD pipeline will:

1. **Unit Tests** ✅
   - Runs all Gradle unit tests
   - Uploads test results

2. **Build & Push Docker Image** ✅
   - Builds JAR file
   - Creates Docker image with tag: `sindhu030/user_role_production:user-roles`
   - Pushes to private Docker Hub repository

3. **Deploy to Test Environment** ✅
   - Updates ECS service: `test-user-management-cluster`
   - Waits for deployment completion

4. **Health Check on Test Environment** ✅
   - Tests API endpoints
   - Verifies ECS service health
   - Sends CloudWatch metrics

5. **Integration Tests on Test Environment** ✅
   - Runs end-to-end tests
   - Tests API functionality

6. **Deploy to Production Environment** ✅
   - Updates ECS service: `production-user-management-cluster`
   - Waits for deployment completion

7. **Post-deployment Tests** ✅
   - Runs health checks on production
   - Executes smoke tests

## 📊 Pipeline Flow for user-roles Branch

```
user-roles branch push
    ↓
Unit Tests
    ↓
Build & Push (tag: user-roles)
    ↓
Deploy to Test Environment
    ↓
Health Check Test Environment
    ↓
Integration Tests Test Environment
    ↓
Deploy to Production Environment
    ↓
Post-deployment Tests Production
```

## 🔧 Environment Configuration

### Test Environment
- **ECS Cluster**: `test-user-management-cluster`
- **ECS Service**: `test-user-management-service`
- **API Gateway**: `test-user-management-api`
- **Image Tag**: `user-roles`

### Production Environment
- **ECS Cluster**: `production-user-management-cluster`
- **ECS Service**: `production-user-management-service`
- **API Gateway**: `production-user-management-api`
- **Image Tag**: `user-roles`

## 📋 Prerequisites

Make sure you have these GitHub secrets configured:

```bash
DOCKERHUB_USERNAME=sindhu030
DOCKERHUB_TOKEN=your-docker-hub-token
AWS_ROLE_TO_ASSUME=arn:aws:iam::YOUR-ACCOUNT-ID:role/GitHubActionsRole
AWS_REGION=us-east-1
```

## 🚀 Deployment Commands

### Deploy Infrastructure (One-time setup)

#### Test Environment
```bash
aws cloudformation deploy \
  --template-file cloudformation-template-test.yml \
  --stack-name test-user-management-stack \
  --parameter-overrides \
    ImageUrl=sindhu030/user_role_production:user-roles \
    DBHost=your-test-db-endpoint \
    DBPassword=your-test-db-password \
  --capabilities CAPABILITY_IAM
```

#### Production Environment
```bash
aws cloudformation deploy \
  --template-file cloudformation-template.yml \
  --stack-name production-user-management-stack \
  --parameter-overrides \
    ImageUrl=sindhu030/user_role_production:user-roles \
    DBHost=your-production-db-endpoint \
    DBPassword=your-production-db-password \
  --capabilities CAPABILITY_IAM
```

### Deploy Application (Automatic)
```bash
# Just push to user-roles branch
git push origin user-roles
```

## 🔍 Monitoring

### Check Pipeline Status
1. Go to **Actions** tab in GitHub
2. Look for "CI/CD Pipeline - User Management Service"
3. Click on the latest run to see detailed logs

### Check Deployments
```bash
# Check test environment
aws ecs describe-services \
  --cluster test-user-management-cluster \
  --services test-user-management-service

# Check production environment
aws ecs describe-services \
  --cluster production-user-management-cluster \
  --services production-user-management-service
```

### Test API Endpoints
```bash
# Get test API URL
TEST_API=$(aws apigatewayv2 get-apis --query 'Items[?Name==`test-user-management-api`].ApiEndpoint' --output text)
echo "Test API: $TEST_API"

# Get production API URL
PROD_API=$(aws apigatewayv2 get-apis --query 'Items[?Name==`production-user-management-api`].ApiEndpoint' --output text)
echo "Production API: $PROD_API"

# Test health endpoints
curl https://$TEST_API/actuator/health
curl https://$PROD_API/actuator/health
```

## 🐛 Troubleshooting

### Common Issues

1. **Pipeline Fails on Unit Tests**
   - Check test code for errors
   - Review test logs in GitHub Actions

2. **Docker Push Fails**
   - Verify Docker Hub credentials
   - Check repository permissions

3. **ECS Deployment Fails**
   - Check IAM permissions
   - Verify cluster and service names
   - Review ECS service logs

4. **Health Check Fails**
   - Check API Gateway configuration
   - Verify ECS service is running
   - Review CloudWatch logs

### Debug Commands

```bash
# Check ECS service status
aws ecs describe-services --cluster test-user-management-cluster --services test-user-management-service
aws ecs describe-services --cluster production-user-management-cluster --services production-user-management-service

# Check API Gateway
aws apigatewayv2 get-apis

# View CloudWatch logs
aws logs describe-log-groups --log-group-name-prefix "/ecs/"

# Test health check manually
pwsh ./health-check-enhanced.ps1 -Environment test
pwsh ./health-check-enhanced.ps1 -Environment production
```

## 📈 Benefits of user-roles Branch Deployment

1. **Full Pipeline**: Runs both test and production deployments
2. **Comprehensive Testing**: Unit tests, integration tests, and health checks
3. **Production Ready**: Same pipeline as main branch
4. **Monitoring**: CloudWatch metrics and detailed logging
5. **Rollback**: Easy to revert if issues occur

## 🎯 Next Steps

1. **Set up GitHub secrets** (if not already done)
2. **Deploy infrastructure** using CloudFormation templates
3. **Push to user-roles branch** to trigger deployment
4. **Monitor the pipeline** in GitHub Actions
5. **Verify deployments** using the test commands above

The `user-roles` branch is now configured to run the complete CI/CD pipeline for both test and production environments! 🚀






