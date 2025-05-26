#!/bin/bash

# Exit on error
set -e

echo "🚀 Setting up Test and Production Environments..."

# Get AWS Account ID and Region
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=$(aws configure get region)

echo "Using AWS Account: $AWS_ACCOUNT_ID in Region: $AWS_REGION"

# Function to create ECS cluster
create_cluster() {
    local cluster_name=$1
    echo "Creating ECS Cluster: $cluster_name..."
    aws ecs describe-clusters --clusters $cluster_name || \
    aws ecs create-cluster --cluster-name $cluster_name
}

# Function to create CloudWatch Log Group
create_log_group() {
    local log_group_name=$1
    echo "Creating CloudWatch Log Group: $log_group_name..."
    aws logs create-log-group --log-group-name $log_group_name || true
}

# Function to create security group
create_security_group() {
    local sg_name=$1
    local description=$2
    
    echo "Creating security group: $sg_name..."
    
    # Get default VPC
    VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query 'Vpcs[0].VpcId' --output text)
    
    # Create security group if it doesn't exist
    SECURITY_GROUP_ID=$(aws ec2 describe-security-groups --filters "Name=group-name,Values=$sg_name" --query 'SecurityGroups[0].GroupId' --output text 2>/dev/null || \
    aws ec2 create-security-group --group-name $sg_name --description "$description" --vpc-id $VPC_ID --query 'GroupId' --output text)
    
    # Add inbound rule for port 8080
    aws ec2 authorize-security-group-ingress --group-id $SECURITY_GROUP_ID --protocol tcp --port 8080 --cidr 0.0.0.0/0 2>/dev/null || true
    
    echo "Security Group ID: $SECURITY_GROUP_ID"
}

# Function to create ECS service
create_service() {
    local cluster_name=$1
    local service_name=$2
    local task_definition_file=$3
    local sg_name=$4
    
    echo "Creating ECS service: $service_name in cluster: $cluster_name..."
    
    # Get default VPC and subnet information
    VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query 'Vpcs[0].VpcId' --output text)
    SUBNET_IDS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query 'Subnets[*].SubnetId' --output text | tr '\t' ',')
    SECURITY_GROUP_ID=$(aws ec2 describe-security-groups --filters "Name=group-name,Values=$sg_name" --query 'SecurityGroups[0].GroupId' --output text)
    
    # Update task definition with actual values
    sed -i "s/<YOUR-ACCOUNT-ID>/$AWS_ACCOUNT_ID/g" $task_definition_file
    sed -i "s/<YOUR-REGION>/$AWS_REGION/g" $task_definition_file
    sed -i "s/<IMAGE_TAG>/latest/g" $task_definition_file
    
    # Register task definition
    TASK_DEFINITION_ARN=$(aws ecs register-task-definition --cli-input-json file://$task_definition_file --query 'taskDefinition.taskDefinitionArn' --output text)
    
    # Check if service exists
    if aws ecs describe-services --cluster $cluster_name --services $service_name --query 'services[0].serviceName' --output text 2>/dev/null | grep -q $service_name; then
        echo "Service $service_name already exists, updating..."
        aws ecs update-service --cluster $cluster_name --service $service_name --task-definition $TASK_DEFINITION_ARN
    else
        echo "Creating new service: $service_name..."
        aws ecs create-service \
          --cluster $cluster_name \
          --service-name $service_name \
          --task-definition $TASK_DEFINITION_ARN \
          --desired-count 1 \
          --launch-type FARGATE \
          --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_IDS],securityGroups=[$SECURITY_GROUP_ID],assignPublicIp=ENABLED}"
    fi
}

echo "=== Creating IAM Roles ==="
# Create ECS Task Execution Role
aws iam create-role --role-name ecsTaskExecutionRole --assume-role-policy-document '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}' || true

# Attach required policies to ECS Task Execution Role
aws iam attach-role-policy --role-name ecsTaskExecutionRole --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy || true

# Create ECS Task Role
aws iam create-role --role-name ecsTaskRole --assume-role-policy-document '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}' || true

# Attach required policies to ECS Task Role
aws iam attach-role-policy --role-name ecsTaskRole --policy-arn arn:aws:iam::aws:policy/AmazonECR-FullAccess || true
aws iam attach-role-policy --role-name ecsTaskRole --policy-arn arn:aws:iam::aws:policy/CloudWatchLogsFullAccess || true

echo "=== Creating ECR Repository ==="
aws ecr describe-repositories --repository-names user-management-service || \
aws ecr create-repository --repository-name user-management-service

echo "=== Setting up TEST Environment ==="
create_cluster "user-management-cluster-test"
create_log_group "/ecs/user-management-service-test"
create_security_group "user-management-sg-test" "Security group for User Management Service - Test Environment"
create_service "user-management-cluster-test" "user-management-service-test" "task-definition-test.json" "user-management-sg-test"

echo "=== Setting up PRODUCTION Environment ==="
create_cluster "user-management-cluster-prod"
create_log_group "/ecs/user-management-service-prod"
create_security_group "user-management-sg-prod" "Security group for User Management Service - Production Environment"
create_service "user-management-cluster-prod" "user-management-service-prod" "task-definition.json" "user-management-sg-prod"

echo "✅ Environment setup completed successfully!"
echo ""
echo "📋 Summary:"
echo "- Test Cluster: user-management-cluster-test"
echo "- Test Service: user-management-service-test"
echo "- Production Cluster: user-management-cluster-prod"
echo "- Production Service: user-management-service-prod"
echo ""
echo "🔧 Next Steps:"
echo "1. Set up GitHub Secrets for AWS credentials"
echo "2. Create GitHub Environments (test, production)"
echo "3. Push code to trigger the CI/CD pipeline"
echo ""
echo "📖 GitHub Secrets needed:"
echo "- AWS_ACCESS_KEY_ID"
echo "- AWS_SECRET_ACCESS_KEY"
echo "- AWS_SESSION_TOKEN (if using temporary credentials)"
