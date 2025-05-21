#!/bin/bash

# Exit on error
set -e

echo "Starting deployment process..."

# Get AWS Account ID and Region
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=$(aws configure get region)

echo "Using AWS Account: $AWS_ACCOUNT_ID in Region: $AWS_REGION"

# Create IAM roles if they don't exist
echo "Creating IAM roles..."

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

# Create ECR Repository if it doesn't exist
echo "Creating ECR Repository..."
aws ecr describe-repositories --repository-names user-management-service || \
aws ecr create-repository --repository-name user-management-service

# Create ECS Cluster if it doesn't exist
echo "Creating ECS Cluster..."
aws ecs describe-clusters --clusters user-management-cluster || \
aws ecs create-cluster --cluster-name user-management-cluster

# Create CloudWatch Log Group
echo "Creating CloudWatch Log Group..."
aws logs create-log-group --log-group-name /ecs/user-management-service || true

# Update task definition with actual values
echo "Updating task definition..."
sed -i "s/<YOUR-ACCOUNT-ID>/$AWS_ACCOUNT_ID/g" task-definition.json
sed -i "s/<YOUR-REGION>/$AWS_REGION/g" task-definition.json

# Register task definition
echo "Registering task definition..."
TASK_DEFINITION_ARN=$(aws ecs register-task-definition --cli-input-json file://task-definition.json --query 'taskDefinition.taskDefinitionArn' --output text)

# Get default VPC and subnet information
VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query 'Vpcs[0].VpcId' --output text)
SUBNET_IDS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query 'Subnets[*].SubnetId' --output text | tr '\t' ',')

# Create security group if it doesn't exist
echo "Creating security group..."
SECURITY_GROUP_ID=$(aws ec2 describe-security-groups --filters "Name=group-name,Values=user-management-sg" --query 'SecurityGroups[0].GroupId' --output text || \
aws ec2 create-security-group --group-name user-management-sg --description "Security group for User Management Service" --vpc-id $VPC_ID --query 'GroupId' --output text)

# Add inbound rule for port 8080
aws ec2 authorize-security-group-ingress --group-id $SECURITY_GROUP_ID --protocol tcp --port 8080 --cidr 0.0.0.0/0 || true

# Create or update ECS service
echo "Creating/Updating ECS service..."
aws ecs describe-services --cluster user-management-cluster --services user-management-service || \
aws ecs create-service \
  --cluster user-management-cluster \
  --service-name user-management-service \
  --task-definition $TASK_DEFINITION_ARN \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_IDS],securityGroups=[$SECURITY_GROUP_ID],assignPublicIp=ENABLED}"

# Build and push Docker image
echo "Building and pushing Docker image..."
docker build -t user-management-service .
docker tag user-management-service:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/user-management-service:latest
aws ecr get-login-password | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/user-management-service:latest

# Force new deployment
echo "Forcing new deployment..."
aws ecs update-service --cluster user-management-cluster --service user-management-service --force-new-deployment

echo "Deployment completed successfully!"
echo "You can monitor the deployment in the ECS console or using CloudWatch logs." 