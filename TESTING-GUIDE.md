# Testing Guide: Client Management + User Management Integration

This guide provides step-by-step instructions to test both services together.

## Prerequisites

- Java 17 or higher
- Git
- Two terminal windows

## Step 1: Setup Client Management Service

### 1.1 Clone and Start Client Management Service

```bash
# Terminal 1 - Client Management Service
git clone https://github.com/SindhuJinu09/client-management.git
cd client-management
./gradlew bootRun
```

The service will start on `http://localhost:8080`

### 1.2 Verify Client Management Service is Running

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

## Step 2: Register a Client and Get API Key

### 2.1 Register a New Client

```bash
curl -X POST http://localhost:8080/clients \
-H "Content-Type: application/json" \
-d '{
  "clientName": "User Management Test Client",
  "clientType": "service",
  "description": "Test client for User Management Service integration"
}'
```

Expected response:
```json
{
  "client_id": "12345678-1234-1234-1234-123456789012",
  "api_key": "ak_test_1234567890abcdef1234567890abcdef",
  "client_name": "User Management Test Client",
  "status": "active"
}
```

**⚠️ IMPORTANT: Save the `api_key` value - you'll need it for testing!**

### 2.2 Verify API Key Works

```bash
# Replace YOUR_API_KEY with the actual key from step 2.1
curl -X GET http://localhost:8080/api/validate \
-H "x-api-key: YOUR_API_KEY"
```

Expected response:
```json
{
  "valid": true,
  "client_id": "12345678-1234-1234-1234-123456789012",
  "message": "API key is valid"
}
```

## Step 3: Start User Management Service

### 3.1 Start User Management Service

```bash
# Terminal 2 - User Management Service (in your current directory)
./gradlew bootRun
```

The service will start on `http://localhost:8082`

### 3.2 Verify User Management Service is Running

```bash
curl http://localhost:8082/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

## Step 4: Test the Integration

### 4.1 Test Without API Key (Should Fail)

```bash
curl -X POST http://localhost:8082/user \
-H "Content-Type: application/json" \
-H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
-H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
-H "x-app-client-user-session-uuid: session-12345" \
-H "x-app-trace-id: trace-67890" \
-H "x-app-region-id: us-east-1" \
-d '{
  "username": "testuser1",
  "firstName": "Test",
  "lastName": "User"
}'
```

Expected response (401 Unauthorized):
```json
{
  "error": "Missing API key",
  "message": "x-api-key header is required"
}
```

### 4.2 Test With Invalid API Key (Should Fail)

```bash
curl -X POST http://localhost:8082/user \
-H "Content-Type: application/json" \
-H "x-api-key: invalid-key" \
-H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
-H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
-H "x-app-client-user-session-uuid: session-12345" \
-H "x-app-trace-id: trace-67890" \
-H "x-app-region-id: us-east-1" \
-d '{
  "username": "testuser2",
  "firstName": "Test",
  "lastName": "User"
}'
```

Expected response (401 Unauthorized):
```json
{
  "error": "Invalid API key",
  "message": "The provided API key is invalid or expired"
}
```

### 4.3 Test With Valid API Key (Should Succeed)

```bash
# Replace YOUR_API_KEY with the actual key from step 2.1
curl -X POST http://localhost:8082/user \
-H "Content-Type: application/json" \
-H "x-api-key: YOUR_API_KEY" \
-H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
-H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
-H "x-app-client-user-session-uuid: session-12345" \
-H "x-app-trace-id: trace-67890" \
-H "x-app-region-id: us-east-1" \
-d '{
  "username": "testuser3",
  "firstName": "Test",
  "lastName": "User",
  "phoneInfo": {
    "number": "1234567890",
    "countryCode": "1",
    "verificationStatus": "verified"
  },
  "emailInfo": {
    "email": "testuser3@example.com",
    "verificationStatus": "verified"
  }
}'
```

Expected response (200 OK):
```json
{
  "httpStatus": "OK",
  "userId": "generated-uuid-here",
  "username": "testuser3",
  "status": "Active",
  "message": "User created successfully"
}
```

## Step 5: Test Other User Management Endpoints

### 5.1 Get User by ID

```bash
# Replace YOUR_API_KEY and USER_ID with actual values
curl -X GET http://localhost:8082/user/USER_ID \
-H "Content-Type: application/json" \
-H "x-api-key: YOUR_API_KEY" \
-H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
-H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
-H "x-app-client-user-session-uuid: session-12345" \
-H "x-app-trace-id: trace-67890" \
-H "x-app-region-id: us-east-1"
```

### 5.2 List Users

```bash
# Replace YOUR_API_KEY with actual value
curl -X POST http://localhost:8082/users/filter \
-H "Content-Type: application/json" \
-H "x-api-key: YOUR_API_KEY" \
-H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
-H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
-H "x-app-client-user-session-uuid: session-12345" \
-H "x-app-trace-id: trace-67890" \
-H "x-app-region-id: us-east-1" \
-d '{
  "page": 0,
  "size": 10,
  "sortBy": "username",
  "sortDirection": "ASC"
}'
```

## Step 6: Test API Key Management

### 6.1 Generate New API Key

```bash
# Replace YOUR_API_KEY and CLIENT_ID with actual values
curl -X POST http://localhost:8081/clients/CLIENT_ID/api-keys \
-H "x-api-key: YOUR_API_KEY"
```

### 6.2 Revoke API Key

```bash
# Replace YOUR_API_KEY and API_KEY_ID with actual values
curl -X POST http://localhost:8081/clients/api-keys/API_KEY_ID/revoke \
-H "x-api-key: YOUR_API_KEY"
```

## Troubleshooting

### Common Issues

1. **Connection Refused**
   - Ensure both services are running
   - Check ports 8080 and 8082 are not in use by other applications

2. **API Key Validation Fails**
   - Verify Client Management Service is accessible
   - Check the API key is correct and not expired
   - Ensure no extra spaces in the API key

3. **Database Connection Issues**
   - Check PostgreSQL is running and accessible
   - Verify database credentials in application.properties

### Logs

Check service logs for detailed error information:

```bash
# User Management Service logs
tail -f logs/user-management-service.log

# Client Management Service logs (check their log configuration)
```

## Success Criteria

✅ Client Management Service starts successfully  
✅ User Management Service starts successfully  
✅ API key registration works  
✅ API key validation works  
✅ Requests without API key are rejected (401)  
✅ Requests with invalid API key are rejected (401)  
✅ Requests with valid API key are accepted (200)  
✅ All User Management endpoints work with valid API key  

## Next Steps

Once testing is complete, you can:
- Deploy both services to production
- Set up monitoring and alerting
- Configure load balancing
- Implement API rate limiting
- Add more comprehensive logging
