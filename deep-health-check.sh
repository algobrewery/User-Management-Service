#!/bin/bash

set -e
set -x

API_BASE_URL="http://3.86.190.90:8080/user"
TIMEOUT=5

TIMESTAMP=$(date +%s)
MANAGER_USERNAME="manageruser_$TIMESTAMP"
MANAGER_PHONE="99999${TIMESTAMP: -5}"
MANAGER_EMAIL="manager_${TIMESTAMP}@algobrewery.com"

USERNAME="testuser_$TIMESTAMP"
PHONE_NUMBER="80808${TIMESTAMP: -5}"
EMAIL="testuser_${TIMESTAMP}@algobrewery.com"

# Function to send HTTP requests
http_request() {
  local method=$1
  local url=$2
  local data=$3
  local response
  local userId=""

  >&2 echo "Sending $method request to $url"
  [ -n "$data" ] && >&2 echo "Payload: $data"

  # First check if server is reachable
  if ! curl -s -m $TIMEOUT -o /dev/null "$API_BASE_URL"; then
    >&2 echo "❌ Cannot connect to server at $API_BASE_URL"
    exit 1
  fi

  local curl_opts=(
    "-s"
    "-m" "$TIMEOUT"
    "-X" "$method"
    "-H" "Content-Type: application/json"
    "-H" "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789"
    "-H" "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5"
    "-H" "x-app-client-user-session-uuid: session-12345"
    "-H" "x-app-trace-id: trace-$TIMESTAMP"
    "-H" "x-app-region-id: us-east-1"
  )
  [ -n "$data" ] && curl_opts+=("-d" "$data")
  curl_opts+=("$url")

  response=$(curl "${curl_opts[@]}")
  local curl_exit_code=$?

  if [ $curl_exit_code -eq 28 ]; then
    >&2 echo "❌ Request timed out after ${TIMEOUT} seconds"
    exit 1
  elif [ $curl_exit_code -ne 0 ]; then
    >&2 echo "❌ Curl command failed with exit code $curl_exit_code"
    >&2 echo "Response: $response"
    exit 1
  fi

  local status_code=$(curl -s -m $TIMEOUT -o /dev/null -w "%{http_code}" -X "$method" "$url" \
    -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
    -H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
    -H "x-app-client-user-session-uuid: session-12345" \
    -H "x-app-trace-id: trace-$TIMESTAMP" \
    -H "x-app-region-id: us-east-1")

  >&2 echo "Response Status: $status_code"
  >&2 echo "Response Body: $response"

  if [ "$method" = "POST" ]; then
    userId=$(echo "$response" | grep -o '"userId":"[^"]*"' | head -1 | sed 's/"userId":"\([^"]*\)"/\1/')
    if [ -z "$userId" ]; then
      >&2 echo "❌ Failed to extract userId from response"
      exit 1
    fi
    echo "$userId"
    return 0
  fi

  echo "$response"
}

echo "=== Creating Manager User (reporting manager) ==="
MANAGER_PAYLOAD=$(cat <<EOF
{
  "username": "$MANAGER_USERNAME",
  "firstName": "Manager",
  "middleName": "Healthcheck",
  "lastName": "User",
  "phoneInfo": {
    "number": "$MANAGER_PHONE",
    "countryCode": 91,
    "verificationStatus": "verified"
  },
  "emailInfo": {
    "email": "$MANAGER_EMAIL",
    "verificationStatus": "verified"
  },
  "employmentInfoList": [
    {
      "startDate": "2022-01-01T00:00:00",
      "endDate": "2023-01-01T00:00:00",
      "jobTitle": "Manager",
      "organizationUnit": "QA",
      "reportingManager": "",
      "extensionsData": {
        "employmentType": "FULL_TIME",
        "primaryLocation": "Remote"
      }
    }
  ]
}
EOF
)

MANAGER_ID=$(http_request "POST" "$API_BASE_URL" "$MANAGER_PAYLOAD")
echo "✅ Created manager user with ID: $MANAGER_ID"

echo "=== Creating Test User (with reportingManager as the manager above) ==="
CREATE_PAYLOAD=$(cat <<EOF
{
  "username": "$USERNAME",
  "firstName": "Test",
  "middleName": "Healthcheck",
  "lastName": "User",
  "phoneInfo": {
    "number": "$PHONE_NUMBER",
    "countryCode": 91,
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
      "jobTitle": "Health Check Engineer",
      "organizationUnit": "QA",
      "reportingManager": "$MANAGER_ID",
      "extensionsData": {
        "employmentType": "FULL_TIME",
        "primaryLocation": "Remote"
      }
    }
  ]
}
EOF
)

TEST_USER_ID=$(http_request "POST" "$API_BASE_URL" "$CREATE_PAYLOAD")
echo "✅ Created test user with ID: $TEST_USER_ID"

echo "=== Getting Test User ==="
GET_URL="$API_BASE_URL/$TEST_USER_ID"
http_request "GET" "$GET_URL" ""

echo "=== Updating Test User ==="
UPDATE_PAYLOAD='{
  "firstName": "Updated",
  "lastName": "User"
}'
UPDATE_URL="$API_BASE_URL/$TEST_USER_ID"
http_request "PUT" "$UPDATE_URL" "$UPDATE_PAYLOAD"

echo "=== Deleting Test User ==="
DELETE_URL="$API_BASE_URL/$TEST_USER_ID"
http_request "DELETE" "$DELETE_URL" ""

echo "=== Verifying Test User Deletion ==="
GET_URL="$API_BASE_URL/$TEST_USER_ID"
DELETE_RESPONSE=$(curl -s -m $TIMEOUT -X DELETE "$GET_URL" \
   -H "Content-Type: application/json" \
   -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
   -H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
   -H "x-app-client-user-session-uuid: session-12345" \
   -H "x-app-region-id: us-east-1" \
   -H "x-app-trace-id: trace-$TIMESTAMP")

if echo "$DELETE_RESPONSE" | grep -q '"status":"Inactive"'; then
  echo "✅ User deletion verified"
  echo "Response: $DELETE_RESPONSE"
else
  echo "❌ User deletion verification failed"
  echo "Actual response: $DELETE_RESPONSE"
  exit 1
fi

echo "================================="
echo "✅ All deep health checks passed!"
exit 0