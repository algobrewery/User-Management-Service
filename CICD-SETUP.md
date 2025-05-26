# CI/CD Pipeline Setup Guide

## Overview
This guide will help you set up a complete CI/CD pipeline with test and production environments for the User Management Service.

## Pipeline Flow
```
git push → Unit Tests → Build & Push Image → Deploy to Test → Health Check → Integration Tests → Deploy to Production
```

## Prerequisites
- AWS CLI configured in CloudShell
- GitHub repository with appropriate permissions
- AWS ECS, ECR, and related services access

## Step-by-Step Setup

### 1. Set Up AWS Infrastructure

Run the environment setup script in CloudShell:

```bash
chmod +x setup-environments.sh
./setup-environments.sh
```

This script will create:
- **Test Environment:**
  - ECS Cluster: `user-management-cluster-test`
  - ECS Service: `user-management-service-test`
  - Security Group: `user-management-sg-test`
  - CloudWatch Log Group: `/ecs/user-management-service-test`

- **Production Environment:**
  - ECS Cluster: `user-management-cluster-prod`
  - ECS Service: `user-management-service-prod`
  - Security Group: `user-management-sg-prod`
  - CloudWatch Log Group: `/ecs/user-management-service-prod`

### 2. Set Up GitHub Secrets

In your GitHub repository, go to Settings → Secrets and variables → Actions, and add:

```
AWS_ACCESS_KEY_ID=your_access_key_id
AWS_SECRET_ACCESS_KEY=your_secret_access_key
AWS_SESSION_TOKEN=your_session_token (if using temporary credentials)
```

### 3. Set Up GitHub Environments

1. Go to Settings → Environments in your GitHub repository
2. Create two environments:
   - `test` - No protection rules needed
   - `production` - Add protection rules (require reviews, restrict to main branch)

### 4. Pipeline Stages Explained

#### Stage 1: Unit Tests
- Runs on every push and pull request
- Executes all unit tests using Gradle
- Uploads test results as artifacts

#### Stage 2: Build and Push
- Only runs on main branch pushes
- Builds the Spring Boot application
- Creates Docker image with commit SHA as tag
- Pushes to Amazon ECR

#### Stage 3: Deploy to Test
- Deploys the new image to test environment
- Updates ECS service with new task definition
- Retrieves test environment URL

#### Stage 4: Health Check
- Runs deep health check script against test environment
- Validates all API endpoints
- Ensures service is functioning correctly

#### Stage 5: Integration Tests
- Runs comprehensive integration tests
- Tests all CRUD operations
- Validates error handling

#### Stage 6: Deploy to Production
- Only runs after all previous stages pass
- Deploys to production environment
- Runs basic health check
- Provides deployment summary

## Manual Testing

### Test the Deep Health Check
```bash
chmod +x deep-health-check.sh
# Update the API_BASE_URL in the script to your test environment
./deep-health-check.sh
```

### Test Integration Tests
```bash
chmod +x integration-tests.sh
export TEST_URL="http://your-test-environment:8080"
./integration-tests.sh
```

## Environment URLs

After deployment, you can find your environment URLs in:
- **Test Environment**: Check GitHub Actions logs for the test URL
- **Production Environment**: Check GitHub Actions logs or use your current URL: http://52.87.215.201:8080/

## Monitoring and Logs

### CloudWatch Logs
- Test Environment: `/ecs/user-management-service-test`
- Production Environment: `/ecs/user-management-service-prod`

### ECS Console
- Monitor service health and task status in AWS ECS Console
- Check service events for deployment status

## Troubleshooting

### Common Issues

1. **Task Definition Registration Fails**
   - Check IAM roles exist: `ecsTaskExecutionRole` and `ecsTaskRole`
   - Verify AWS credentials in GitHub Secrets

2. **Service Creation Fails**
   - Ensure security groups allow inbound traffic on port 8080
   - Check subnet configuration and VPC settings

3. **Health Checks Fail**
   - Verify application is running on port 8080
   - Check CloudWatch logs for application errors
   - Ensure database connectivity

4. **Integration Tests Fail**
   - Verify test environment URL is accessible
   - Check required headers are being sent
   - Validate database state

### Useful Commands

```bash
# Check ECS service status
aws ecs describe-services --cluster user-management-cluster-test --services user-management-service-test

# View CloudWatch logs
aws logs tail /ecs/user-management-service-test --follow

# Get service public IP
aws ecs list-tasks --cluster user-management-cluster-test --service-name user-management-service-test
aws ecs describe-tasks --cluster user-management-cluster-test --tasks TASK_ARN
```

## Security Considerations

1. **Database Credentials**: Consider using AWS Secrets Manager for database passwords
2. **Network Security**: Implement proper VPC and security group configurations
3. **IAM Roles**: Follow principle of least privilege for ECS task roles
4. **Environment Separation**: Use separate databases for test and production

## Next Steps

1. **Database Separation**: Set up separate test and production databases
2. **Monitoring**: Implement CloudWatch alarms and dashboards
3. **Backup Strategy**: Set up automated database backups
4. **SSL/TLS**: Configure HTTPS with Application Load Balancer
5. **Auto Scaling**: Configure ECS auto scaling based on metrics
