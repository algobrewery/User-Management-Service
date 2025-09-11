# CI/CD Pipeline Setup Guide

This guide explains how to set up and configure the comprehensive CI/CD pipeline for the User Management Service using GitHub Actions and AWS ECS.

## 🏗️ Pipeline Overview

The CI/CD pipeline follows this flow:

```
1. Unit Tests → 2. Build & Push → 3. Deploy Test → 4. Health Check → 5. Integration Tests → 6. Deploy Production → 7. Post-deployment Tests
```

## 📋 Prerequisites

### 1. GitHub Repository Setup
- Private Docker Hub repository: `sindhu030/user_role_production`
- GitHub repository with Actions enabled
- Required secrets configured (see below)

### 2. AWS Resources
- ECS clusters for test and production environments
- API Gateway endpoints for both environments
- RDS databases for test and production
- IAM roles with appropriate permissions

### 3. Required GitHub Secrets

Add these secrets to your GitHub repository:

```bash
# Docker Hub credentials
DOCKERHUB_USERNAME=sindhu030
DOCKERHUB_TOKEN=your-docker-hub-token

# AWS credentials (OIDC)
AWS_ROLE_TO_ASSUME=arn:aws:iam::123456789012:role/GitHubActionsRole
AWS_REGION=us-east-1
```

## 🔧 Environment Configuration

### Test Environment
- **ECS Cluster**: `test-user-management-cluster`
- **ECS Service**: `test-user-management-service`
- **API Gateway**: `test-user-management-api`
- **Database**: Test RDS instance
- **Image Tag**: `test-latest` or `test-{commit-sha}`

### Production Environment
- **ECS Cluster**: `production-user-management-cluster`
- **ECS Service**: `production-user-management-service`
- **API Gateway**: `production-user-management-api`
- **Database**: Production RDS instance
- **Image Tag**: `latest`

## 🚀 Pipeline Stages

### Stage 1: Unit Tests
- Runs on every push and PR
- Executes Gradle unit tests
- Uploads test results as artifacts
- **Trigger**: All branches and PRs

### Stage 2: Build and Push Docker Image
- Builds JAR file using Gradle
- Creates Docker image with private repository
- Pushes to Docker Hub with appropriate tags
- **Trigger**: All pushes except PRs

### Stage 3: Deploy to Test Environment
- Updates ECS service with new image
- Waits for deployment completion
- **Trigger**: Non-main branches

### Stage 4: Health Check on Test Environment
- Runs comprehensive health checks
- Tests API endpoints
- Verifies ECS service health
- Sends CloudWatch metrics
- **Trigger**: After test deployment

### Stage 5: Integration Tests on Test Environment
- Runs integration tests against test environment
- Tests API functionality end-to-end
- **Trigger**: After successful health check

### Stage 6: Deploy to Production Environment
- Updates production ECS service
- Waits for deployment completion
- **Trigger**: Main branch or manual dispatch

### Stage 7: Post-deployment Tests
- Runs health checks on production
- Executes smoke tests
- **Trigger**: After production deployment

## 📁 File Structure

```
.github/workflows/
├── ci-cd-pipeline.yml          # Main CI/CD pipeline
└── docker-publish.yml          # Legacy workflow (can be removed)

cloudformation-template.yml     # Production environment
cloudformation-template-test.yml # Test environment
health-check-enhanced.ps1       # Enhanced health check script
```

## 🔐 Security Configuration

### Docker Hub Private Repository
1. Create private repository: `sindhu030/user_role_production`
2. Generate access token in Docker Hub
3. Add credentials to GitHub secrets

### AWS IAM Role Setup
Create an IAM role for GitHub Actions with these permissions:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "arn:aws:iam::123456789012:oidc-provider/token.actions.githubusercontent.com"
            },
            "Action": "sts:AssumeRoleWithWebIdentity",
            "Condition": {
                "StringEquals": {
                    "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
                },
                "StringLike": {
                    "token.actions.githubusercontent.com:sub": "repo:your-org/your-repo:*"
                }
            }
        }
    ]
}
```

Required policies:
- `AmazonECS_FullAccess`
- `AmazonAPIGatewayAdministrator`
- `CloudWatchLogsFullAccess`
- `AmazonRDSReadOnlyAccess`

## 🚀 Deployment Process

### Automatic Deployment
1. **Feature Branch**: Push to any branch → Runs unit tests, builds image, deploys to test
2. **Main Branch**: Push to main → Full pipeline including production deployment

### Manual Deployment
1. Go to Actions tab in GitHub
2. Select "CI/CD Pipeline - User Management Service"
3. Click "Run workflow"
4. Choose environment (test/production)
5. Click "Run workflow"

## 📊 Monitoring and Metrics

### CloudWatch Metrics
The pipeline sends metrics to CloudWatch:
- `UserManagementService/HealthCheck/Test/*`
- `UserManagementService/HealthCheck/Production/*`

### Health Check Metrics
- `HealthCheckTest`: Individual test results
- `HealthCheckDuration`: Test execution time
- `ECSRunningTasks`: ECS service health
- `HealthCheckOverall`: Overall health status

## 🔧 Customization

### Adding New Test Environments
1. Create new CloudFormation template
2. Add environment config to health check script
3. Update pipeline workflow
4. Add environment-specific secrets

### Modifying Health Checks
Edit `health-check-enhanced.ps1`:
- Add new API endpoint tests
- Modify ECS health checks
- Update CloudWatch metrics

### Changing Deployment Strategy
Modify the workflow file:
- Add approval gates
- Implement blue-green deployment
- Add rollback mechanisms

## 🐛 Troubleshooting

### Common Issues

1. **Docker Push Fails**
   - Check Docker Hub credentials
   - Verify repository permissions
   - Ensure image name matches repository

2. **ECS Deployment Fails**
   - Check IAM permissions
   - Verify cluster and service names
   - Check task definition compatibility

3. **Health Check Fails**
   - Verify API Gateway URLs
   - Check ECS service status
   - Review CloudWatch logs

4. **Integration Tests Fail**
   - Check test environment connectivity
   - Verify test data setup
   - Review test configuration

### Debug Commands

```bash
# Check ECS service status
aws ecs describe-services --cluster test-user-management-cluster --services test-user-management-service

# Check API Gateway
aws apigatewayv2 get-apis

# View CloudWatch logs
aws logs describe-log-groups --log-group-name-prefix "/ecs/"

# Test health check locally
pwsh ./health-check-enhanced.ps1 -Environment test
```

## 📈 Best Practices

1. **Branch Strategy**
   - Use feature branches for development
   - Merge to main for production releases
   - Use tags for version releases

2. **Security**
   - Use OIDC for AWS authentication
   - Rotate Docker Hub tokens regularly
   - Monitor CloudWatch for anomalies

3. **Monitoring**
   - Set up CloudWatch alarms
   - Monitor pipeline execution times
   - Track deployment success rates

4. **Testing**
   - Run tests in isolated environments
   - Use realistic test data
   - Implement proper cleanup

## 🔄 Maintenance

### Regular Tasks
- Update base Docker images
- Review and update dependencies
- Monitor pipeline performance
- Clean up old Docker images

### Monitoring
- Check CloudWatch dashboards
- Review pipeline execution logs
- Monitor application metrics
- Track error rates

## 📞 Support

For issues or questions:
1. Check GitHub Actions logs
2. Review CloudWatch logs
3. Test health checks manually
4. Create GitHub issue with details

## 🎯 Next Steps

1. Set up GitHub secrets
2. Deploy CloudFormation templates
3. Test the pipeline with a feature branch
4. Configure monitoring and alerts
5. Train team on the deployment process

