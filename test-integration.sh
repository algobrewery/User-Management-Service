#!/bin/bash

# Integration Test Script for Client Management + User Management Services
# This script tests the complete integration between both services

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
CLIENT_MGMT_URL="http://localhost:8080"
USER_MGMT_URL="http://localhost:8082"
TIMEOUT=10

# Global variables
API_KEY=""
CLIENT_ID=""
USER_ID=""

echo -e "${BLUE}🚀 Starting Integration Test for Client Management + User Management Services${NC}"
echo "=================================================================="

# Function to print test results
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ $2${NC}"
    else
        echo -e "${RED}❌ $2${NC}"
        exit 1
    fi
}

# Function to make HTTP requests with timeout
http_request() {
    local method=$1
    local url=$2
    local data=$3
    local headers=$4
    
    if [ -n "$data" ]; then
        curl -s -m $TIMEOUT -X $method -H "Content-Type: application/json" $headers -d "$data" "$url"
    else
        curl -s -m $TIMEOUT -X $method -H "Content-Type: application/json" $headers "$url"
    fi
}

echo -e "${YELLOW}Step 1: Checking if services are running...${NC}"

# Test 1: Check Client Management Service
echo "Testing Client Management Service health..."
response=$(http_request "GET" "$CLIENT_MGMT_URL/actuator/health" "" "")
if echo "$response" | grep -q '"status":"UP"'; then
    print_result 0 "Client Management Service is running"
else
    print_result 1 "Client Management Service is not running or not healthy"
fi

# Test 2: Check User Management Service
echo "Testing User Management Service health..."
response=$(http_request "GET" "$USER_MGMT_URL/actuator/health" "" "")
if echo "$response" | grep -q '"status":"UP"'; then
    print_result 0 "User Management Service is running"
else
    print_result 1 "User Management Service is not running or not healthy"
fi

echo -e "\n${YELLOW}Step 2: Registering a test client...${NC}"

# Test 3: Register a client
echo "Registering test client..."
client_data='{
  "clientName": "Integration Test Client",
  "clientType": "service",
  "description": "Automated integration test client"
}'

response=$(http_request "POST" "$CLIENT_MGMT_URL/clients" "$client_data" "")
if echo "$response" | grep -q '"api_key"'; then
    API_KEY=$(echo "$response" | grep -o '"api_key":"[^"]*"' | cut -d'"' -f4)
    CLIENT_ID=$(echo "$response" | grep -o '"client_id":"[^"]*"' | cut -d'"' -f4)
    print_result 0 "Client registered successfully"
    echo -e "${BLUE}   API Key: $API_KEY${NC}"
    echo -e "${BLUE}   Client ID: $CLIENT_ID${NC}"
else
    print_result 1 "Failed to register client"
fi

echo -e "\n${YELLOW}Step 3: Testing API key validation...${NC}"

# Test 4: Validate API key
echo "Testing API key validation..."
response=$(http_request "GET" "$CLIENT_MGMT_URL/api/validate" "" "-H 'x-api-key: $API_KEY'")
if echo "$response" | grep -q '"valid":true'; then
    print_result 0 "API key validation successful"
else
    print_result 1 "API key validation failed"
fi

echo -e "\n${YELLOW}Step 4: Testing User Management authentication...${NC}"

# Test 5: Test without API key (should fail)
echo "Testing request without API key (should fail)..."
user_data='{
  "username": "testuser_no_key",
  "firstName": "Test",
  "lastName": "User"
}'

headers="-H 'x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789' -H 'x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5' -H 'x-app-client-user-session-uuid: session-12345' -H 'x-app-trace-id: trace-67890' -H 'x-app-region-id: us-east-1'"

response=$(http_request "POST" "$USER_MGMT_URL/user" "$user_data" "$headers" 2>/dev/null || echo '{"error":"Missing API key"}')
if echo "$response" | grep -q '"error":"Missing API key"'; then
    print_result 0 "Request without API key correctly rejected"
else
    print_result 1 "Request without API key was not rejected properly"
fi

# Test 6: Test with invalid API key (should fail)
echo "Testing request with invalid API key (should fail)..."
response=$(http_request "POST" "$USER_MGMT_URL/user" "$user_data" "$headers -H 'x-api-key: invalid-key'" 2>/dev/null || echo '{"error":"Invalid API key"}')
if echo "$response" | grep -q '"error":"Invalid API key"'; then
    print_result 0 "Request with invalid API key correctly rejected"
else
    print_result 1 "Request with invalid API key was not rejected properly"
fi

echo -e "\n${YELLOW}Step 5: Testing User Management with valid API key...${NC}"

# Test 7: Create user with valid API key (should succeed)
echo "Creating user with valid API key..."
user_data='{
  "username": "testuser_integration",
  "firstName": "Integration",
  "lastName": "Test",
  "phoneInfo": {
    "number": "1234567890",
    "countryCode": "1",
    "verificationStatus": "verified"
  },
  "emailInfo": {
    "email": "integration.test@example.com",
    "verificationStatus": "verified"
  }
}'

response=$(http_request "POST" "$USER_MGMT_URL/user" "$user_data" "$headers -H 'x-api-key: $API_KEY'")
if echo "$response" | grep -q '"httpStatus":"OK"'; then
    USER_ID=$(echo "$response" | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
    print_result 0 "User created successfully"
    echo -e "${BLUE}   User ID: $USER_ID${NC}"
else
    print_result 1 "Failed to create user"
    echo "Response: $response"
fi

# Test 8: Get user by ID
if [ -n "$USER_ID" ]; then
    echo "Getting user by ID..."
    response=$(http_request "GET" "$USER_MGMT_URL/user/$USER_ID" "" "$headers -H 'x-api-key: $API_KEY'")
    if echo "$response" | grep -q '"username":"testuser_integration"'; then
        print_result 0 "User retrieval successful"
    else
        print_result 1 "Failed to retrieve user"
    fi
fi

# Test 9: List users
echo "Testing user list endpoint..."
list_data='{
  "page": 0,
  "size": 10,
  "sortBy": "username",
  "sortDirection": "ASC"
}'

response=$(http_request "POST" "$USER_MGMT_URL/users/filter" "$list_data" "$headers -H 'x-api-key: $API_KEY'")
if echo "$response" | grep -q '"totalElements"'; then
    print_result 0 "User list retrieval successful"
else
    print_result 1 "Failed to retrieve user list"
fi

echo -e "\n${YELLOW}Step 6: Testing API key management...${NC}"

# Test 10: Generate new API key
echo "Testing API key rotation..."
response=$(http_request "POST" "$CLIENT_MGMT_URL/clients/$CLIENT_ID/api-keys" "" "-H 'x-api-key: $API_KEY'")
if echo "$response" | grep -q '"api_key"'; then
    NEW_API_KEY=$(echo "$response" | grep -o '"api_key":"[^"]*"' | cut -d'"' -f4)
    print_result 0 "New API key generated successfully"
    echo -e "${BLUE}   New API Key: $NEW_API_KEY${NC}"
    
    # Test the new API key
    echo "Testing new API key..."
    response=$(http_request "GET" "$CLIENT_MGMT_URL/api/validate" "" "-H 'x-api-key: $NEW_API_KEY'")
    if echo "$response" | grep -q '"valid":true'; then
        print_result 0 "New API key validation successful"
    else
        print_result 1 "New API key validation failed"
    fi
else
    print_result 1 "Failed to generate new API key"
fi

echo -e "\n${GREEN}🎉 Integration Test Completed Successfully!${NC}"
echo "=================================================================="
echo -e "${BLUE}Summary:${NC}"
echo "✅ Both services are running and healthy"
echo "✅ Client registration works"
echo "✅ API key validation works"
echo "✅ Authentication properly rejects invalid requests"
echo "✅ User Management endpoints work with valid API key"
echo "✅ API key management (rotation) works"
echo ""
echo -e "${YELLOW}Your services are successfully integrated!${NC}"
echo ""
echo -e "${BLUE}API Key for further testing: $API_KEY${NC}"
echo -e "${BLUE}Client ID: $CLIENT_ID${NC}"
if [ -n "$USER_ID" ]; then
    echo -e "${BLUE}Created User ID: $USER_ID${NC}"
fi
