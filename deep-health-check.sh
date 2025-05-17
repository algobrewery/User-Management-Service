#!/bin/bash

# Configuration
API_BASE_URL="http://localhost:8080/user"

# Generate dynamic test data
TIMESTAMP=$(date +%s)
USERNAME="testuser_$TIMESTAMP"
PHONE_NUMBER="808080${TIMESTAMP: -6}"  # Last 6 digits of timestamp
EMAIL="testuser_${TIMESTAMP}@algobrewery.com"

# Headers
HEADERS=(
  "-H \"Content-Type: application/json\""
  "-H \"x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789\""
  "-H \"x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5\""
  "-H \"x-app-client-user-session-uuid: session-12345\""
  "-H \"x-app-trace-id: trace-${TIMESTAMP}\""
  "-H \"x-app-region-id: us-east-1\""
)

# Exit on error
set -e

# Helper function to make HTTP requests
http_request() {
  local method=$1
  local url=$2
  local data=$3
  local response

  echo "Sending $method request to $url"
  echo "Payload: $data"

  if [ -n "$data" ]; then
    response=$(curl -s -X "$method" "$url" \
      -H "Content-Type: application/json" \
      -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
      -H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
      -H "x-app-client-user-session-uuid: session-12345" \
      -H "x-app-trace-id: trace-$TIMESTAMP" \
      -H "x-app-region-id: us-east-1" \
      -d "$data" \
      -w "\n%{http_code}")
  else
    response=$(curl -s -X "$method" "$url" \
      -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
      -H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
      -H "x-app-client-user-session-uuid: session-12345" \
      -H "x-app-trace-id: trace-$TIMESTAMP" \
      -H "x-app-region-id: us-east-1" \
      -w "\n%{http_code}")
  fi

  # Separate body and status code
  local body=$(echo "$response" | sed '$d')
  local status_code=$(echo "$response" | tail -n1)

  echo "Response: $status_code"
  echo "$body" | jq '.' 2>/dev/null || echo "$body"

  if [ "$status_code" -ge 400 ]; then
    echo "❌ Request failed with status $status_code"
    exit 1
  fi

  echo "$body"
}

### Test 1: Create User
echo "=== Creating Test User ==="
CREATE_PAYLOAD=$(cat <<EOF
{
  "username": "$USERNAME",
  "firstName": "Test",
  "middleName": "Healthcheck",
  "lastName": "User",
  "phoneInfo": {
    "number": "$PHONE_NUMBER",
    "countryCode": "91",
    "verificationStatus": "verified"
  },
  "emailInfo": {
    "email": "$EMAIL",
    "verificationStatus": "verified"
  },
  "employmentInfoList": [
    {
      "startDate": "2022-02-12T00:00:00",
      "endDate": "2022-11-12T00:00:00",
      "jobTitle": "Devops intern",
      "organizationUnit": "Technology",
      "reportingManager": "790b5bc8-820d-4a68-a12d-550cfaca14d5",
      "extensionsData": {
        "employmentType": "INTERN",
        "primaryLocation": "Bangalore"
      }
    }
  ]
}
EOF
)

CREATE_RESPONSE=$(http_request "POST" "$API_BASE_URL" "$CREATE_P