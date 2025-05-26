#!/bin/bash

set -e
set -x

# Integration Test Script for User Management Service
# This script runs comprehensive integration tests against the deployed service

API_BASE_URL="${TEST_URL:-http://localhost:8080}/user"
TIMEOUT=10
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

echo "🧪 Starting Integration Tests..."
echo "API Base URL: $API_BASE_URL"
echo "Timeout: ${TIMEOUT}s"
echo "================================="

# Function to make HTTP requests
http_request() {
    local method=$1
    local url=$2
    local payload=$3
    local expected_status=${4:-200}
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo "🔍 Testing: $method $url"
    
    if [ -n "$payload" ]; then
        response=$(curl -s -w "\n%{http_code}" -m $TIMEOUT -X $method "$url" \
           -H "Content-Type: application/json" \
           -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
           -H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
           -H "x-app-client-user-session-uuid: session-12345" \
           -H "x-app-region-id: us-east-1" \
           -H "x-app-trace-id: trace-$(date +%s)" \
           -d "$payload")
    else
        response=$(curl -s -w "\n%{http_code}" -m $TIMEOUT -X $method "$url" \
           -H "Content-Type: application/json" \
           -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
           -H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
           -H "x-app-client-user-session-uuid: session-12345" \
           -H "x-app-region-id: us-east-1" \
           -H "x-app-trace-id: trace-$(date +%s)")
    fi
    
    # Extract HTTP status code (last line)
    http_code=$(echo "$response" | tail -n1)
    # Extract response body (all lines except last)
    response_body=$(echo "$response" | head -n -1)
    
    echo "Response Code: $http_code"
    echo "Response Body: $response_body"
    
    if [ "$http_code" -eq "$expected_status" ]; then
        echo "✅ Test PASSED"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        
        # Extract user ID if this is a create request
        if [ "$method" = "POST" ] && [ "$http_code" -eq "201" ]; then
            USER_ID=$(echo "$response_body" | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
            echo "Created User ID: $USER_ID"
            echo "$USER_ID" > /tmp/integration_test_user_id.txt
        fi
        
        return 0
    else
        echo "❌ Test FAILED - Expected: $expected_status, Got: $http_code"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# Test 1: Health Check (if available)
echo "=== Test 1: Health Check ==="
if curl -f -s "${API_BASE_URL%/user}/actuator/health" >/dev/null 2>&1; then
    echo "✅ Health endpoint available"
    PASSED_TESTS=$((PASSED_TESTS + 1))
elif curl -f -s "${API_BASE_URL%/user}/health" >/dev/null 2>&1; then
    echo "✅ Health endpoint available"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo "⚠️ Health endpoint not available (this is okay)"
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

# Test 2: Create User
echo "=== Test 2: Create User ==="
TIMESTAMP=$(date +%s)
USERNAME="integrationtest_$TIMESTAMP"
PHONE_NUMBER="555${TIMESTAMP: -7}"
EMAIL="integration_${TIMESTAMP}@test.com"

CREATE_PAYLOAD=$(cat <<EOF
{
  "username": "$USERNAME",
  "firstName": "Integration",
  "middleName": "Test",
  "lastName": "User",
  "phoneInfo": {
    "number": "$PHONE_NUMBER",
    "countryCode": "1",
    "verificationStatus": "verified"
  },
  "emailInfo": {
    "email": "$EMAIL",
    "verificationStatus": "verified"
  },
  "employmentInfoList": [
    {
      "startDate": "2023-01-01T00:00:00",
      "endDate": "2024-12-31T00:00:00",
      "jobTitle": "Integration Test Engineer",
      "organizationUnit": "QA",
      "reportingManager": "790b5bc8-820d-4a68-a12d-550cfaca14d5",
      "extensionsData": {
        "employmentType": "FULL_TIME",
        "primaryLocation": "Remote"
      }
    }
  ]
}
EOF
)

http_request "POST" "$API_BASE_URL" "$CREATE_PAYLOAD" 201

# Get the created user ID
if [ -f /tmp/integration_test_user_id.txt ]; then
    TEST_USER_ID=$(cat /tmp/integration_test_user_id.txt)
    echo "Using User ID for subsequent tests: $TEST_USER_ID"
else
    echo "❌ Failed to get user ID from create response"
    exit 1
fi

# Test 3: Get User by ID
echo "=== Test 3: Get User by ID ==="
http_request "GET" "$API_BASE_URL/$TEST_USER_ID" "" 200

# Test 4: Update User
echo "=== Test 4: Update User ==="
UPDATE_PAYLOAD='{
  "firstName": "Updated",
  "lastName": "IntegrationUser",
  "phoneInfo": {
    "number": "5551234567",
    "countryCode": "1",
    "verificationStatus": "verified"
  }
}'

http_request "PUT" "$API_BASE_URL/$TEST_USER_ID" "$UPDATE_PAYLOAD" 200

# Test 5: Get Updated User
echo "=== Test 5: Verify User Update ==="
http_request "GET" "$API_BASE_URL/$TEST_USER_ID" "" 200

# Test 6: List Users (if endpoint exists)
echo "=== Test 6: List Users ==="
LIST_PAYLOAD='{
  "pageNumber": 0,
  "pageSize": 10
}'

# This might return 404 if endpoint doesn't exist, which is okay
http_request "POST" "$API_BASE_URL/list" "$LIST_PAYLOAD" 200 || echo "⚠️ List endpoint not available"

# Test 7: Delete/Deactivate User
echo "=== Test 7: Delete/Deactivate User ==="
http_request "DELETE" "$API_BASE_URL/$TEST_USER_ID" "" 200

# Test 8: Verify User Deletion
echo "=== Test 8: Verify User Deletion ==="
# After deletion, user should still exist but be inactive
http_request "GET" "$API_BASE_URL/$TEST_USER_ID" "" 200

# Test 9: Error Handling - Get Non-existent User
echo "=== Test 9: Error Handling - Non-existent User ==="
FAKE_UUID="00000000-0000-0000-0000-000000000000"
http_request "GET" "$API_BASE_URL/$FAKE_UUID" "" 404 || echo "⚠️ Error handling test - expected 404"

# Test 10: Error Handling - Invalid Request
echo "=== Test 10: Error Handling - Invalid Create Request ==="
INVALID_PAYLOAD='{"invalid": "data"}'
http_request "POST" "$API_BASE_URL" "$INVALID_PAYLOAD" 400 || echo "⚠️ Error handling test - expected 400"

# Cleanup
rm -f /tmp/integration_test_user_id.txt

# Summary
echo "================================="
echo "🧪 Integration Test Summary"
echo "================================="
echo "Total Tests: $TOTAL_TESTS"
echo "Passed: $PASSED_TESTS"
echo "Failed: $FAILED_TESTS"
echo "Success Rate: $(( PASSED_TESTS * 100 / TOTAL_TESTS ))%"

if [ $FAILED_TESTS -eq 0 ]; then
    echo "✅ All integration tests passed!"
    exit 0
else
    echo "❌ Some integration tests failed!"
    exit 1
fi
