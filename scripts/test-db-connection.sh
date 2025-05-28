#!/bin/bash

# Database Connection Test Script
echo "🔍 Testing Database Connections..."

# Test Database Details
TEST_HOST="nucleus-staging-1.culss6gmw8na.us-east-1.rds.amazonaws.com"
TEST_USER="nucleus_staging"
TEST_PASS="nucleus_staging"
TEST_DB="nucleus_staging_db"

# Production Database Details
PROD_HOST="user-management-prod-db.culss6gmw8na.us-east-1.rds.amazonaws.com"
PROD_USER="postgres"
PROD_PASS="tasksilodbtest"
PROD_DB="usermanagement_prod"

echo ""
echo "🧪 Testing TEST Database Connection..."
echo "Host: $TEST_HOST"
echo "Database: $TEST_DB"
echo "User: $TEST_USER"

# Test connection using psql (if available)
if command -v psql &> /dev/null; then
    echo "Using psql to test connection..."
    PGPASSWORD=$TEST_PASS psql -h $TEST_HOST -U $TEST_USER -d $TEST_DB -c "SELECT version();" -c "\q"
    if [ $? -eq 0 ]; then
        echo "✅ TEST Database connection successful!"
    else
        echo "❌ TEST Database connection failed!"
    fi
else
    echo "⚠️ psql not found. Using telnet to test connectivity..."
    timeout 5 bash -c "</dev/tcp/$TEST_HOST/5432" && echo "✅ TEST Database port is reachable" || echo "❌ TEST Database port is not reachable"
fi

echo ""
echo "🚀 Testing PRODUCTION Database Connection..."
echo "Host: $PROD_HOST"
echo "Database: $PROD_DB"
echo "User: $PROD_USER"

# Test connection using psql (if available)
if command -v psql &> /dev/null; then
    echo "Using psql to test connection..."
    PGPASSWORD=$PROD_PASS psql -h $PROD_HOST -U $PROD_USER -d $PROD_DB -c "SELECT version();" -c "\q"
    if [ $? -eq 0 ]; then
        echo "✅ PRODUCTION Database connection successful!"
    else
        echo "❌ PRODUCTION Database connection failed!"
    fi
else
    echo "⚠️ psql not found. Using telnet to test connectivity..."
    timeout 5 bash -c "</dev/tcp/$PROD_HOST/5432" && echo "✅ PRODUCTION Database port is reachable" || echo "❌ PRODUCTION Database port is not reachable"
fi

echo ""
echo "🎯 Database Connection Test Complete!"
