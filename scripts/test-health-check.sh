#!/bin/bash

# Test Health Check Script
# Tests the deep health check against both environments

set -e

echo "🏥 Testing Health Check Scripts..."

# Test Environment URL (replace with actual test URL)
TEST_URL="http://your-test-url:8080"

# Production Environment URL (replace with actual prod URL)
PROD_URL="http://your-prod-url:8080"

echo ""
echo "🧪 Testing against TEST environment..."
if [ "$TEST_URL" != "http://your-test-url:8080" ]; then
    # Update health check script for test
    sed "s|API_BASE_URL=\".*\"|API_BASE_URL=\"$TEST_URL/user\"|g" deep-health-check.sh > test-health-check-temp.sh
    chmod +x test-health-check-temp.sh
    
    echo "Running health check against test environment..."
    ./test-health-check-temp.sh
    
    rm test-health-check-temp.sh
    echo "✅ Test environment health check passed!"
else
    echo "⚠️ Test URL not configured, skipping test environment check"
fi

echo ""
echo "🚀 Testing against PRODUCTION environment..."
if [ "$PROD_URL" != "http://your-prod-url:8080" ]; then
    # Update health check script for production
    sed "s|API_BASE_URL=\".*\"|API_BASE_URL=\"$PROD_URL/user\"|g" deep-health-check.sh > prod-health-check-temp.sh
    chmod +x prod-health-check-temp.sh
    
    echo "⚠️ WARNING: This will test against PRODUCTION!"
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        ./prod-health-check-temp.sh
        echo "✅ Production environment health check passed!"
    else
        echo "❌ Production health check skipped"
    fi
    
    rm prod-health-check-temp.sh
else
    echo "⚠️ Production URL not configured, skipping production check"
fi

echo ""
echo "🎯 Health Check Testing Complete!"
