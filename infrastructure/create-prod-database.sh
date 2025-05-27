#!/bin/bash

# Create Production Database for User Management Service
set -e

# Configuration
DB_INSTANCE_IDENTIFIER="user-management-prod-db"
DB_NAME="usermanagement_prod"
DB_USERNAME="admin"
DB_PASSWORD="YourSecureProductionPassword123!"  # Change this to your preferred password!
DB_INSTANCE_CLASS="db.t3.micro"  # Start small, can scale up
ENGINE="postgres"
ENGINE_VERSION="13.7"
ALLOCATED_STORAGE="20"
STORAGE_TYPE="gp2"
VPC_SECURITY_GROUP_ID="sg-0baa70783b9c7ed67"  # Use your existing security group
DB_SUBNET_GROUP="default"  # Or create a custom one

echo "Creating Production Database: $DB_INSTANCE_IDENTIFIER"

# Create RDS instance
aws rds create-db-instance \
    --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" \
    --db-instance-class "$DB_INSTANCE_CLASS" \
    --engine "$ENGINE" \
    --engine-version "$ENGINE_VERSION" \
    --master-username "$DB_USERNAME" \
    --master-user-password "$DB_PASSWORD" \
    --allocated-storage "$ALLOCATED_STORAGE" \
    --storage-type "$STORAGE_TYPE" \
    --db-name "$DB_NAME" \
    --vpc-security-group-ids "$VPC_SECURITY_GROUP_ID" \
    --db-subnet-group-name "$DB_SUBNET_GROUP" \
    --backup-retention-period 7 \
    --multi-az \
    --storage-encrypted \
    --deletion-protection \
    --enable-performance-insights \
    --tags Key=Environment,Value=Production Key=Application,Value=UserManagement

echo "Database creation initiated. This will take 10-15 minutes..."

# Wait for database to be available
echo "Waiting for database to become available..."
aws rds wait db-instance-available --db-instance-identifier "$DB_INSTANCE_IDENTIFIER"

# Get database endpoint
DB_ENDPOINT=$(aws rds describe-db-instances \
    --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" \
    --query 'DBInstances[0].Endpoint.Address' \
    --output text)

echo "✅ Production Database Created Successfully!"
echo "Database Endpoint: $DB_ENDPOINT"
echo "Database Name: $DB_NAME"
echo "Username: $DB_USERNAME"
echo ""
echo "Next Steps:"
echo "1. Update your production ECS task definition with these database credentials"
echo "2. Create database schema using Flyway migrations"
echo "3. Test connection from your application"
echo ""
echo "Connection String: jdbc:postgresql://$DB_ENDPOINT:5432/$DB_NAME"
