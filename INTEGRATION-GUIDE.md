# Integration Guide: User Management ↔ Client Management Services

This guide explains how to integrate the User Management Service with the Client Management Service for API key authentication.

## Overview

The integration provides:
- **API Key Authentication**: All User Management endpoints require valid API keys
- **Centralized Client Management**: API keys are managed by the Client Management Service
- **Secure Communication**: Services communicate over HTTP with proper authentication

## Architecture

```
Client Application
       ↓ (with x-api-key header)
User Management Service
       ↓ (validates API key)
Client Management Service
```

## Setup Instructions

### 1. Start Client Management Service

First, clone and start your Client Management Service:

```bash
git clone https://github.com/SindhuJinu09/client-management.git
cd client-management
./gradlew bootRun
```

The service will start on `http://localhost:8080`

### 2. Register a Client and Get API Key

Register a new client to get an API key:

```bash
curl -X POST http://localhost:8080/clients \
-H "Content-Type: application/json" \
-d '{
  "clientName": "User Management Client",
  "clientType": "service",
  "description": "API client for User Management Service"
}'
```

Response will include your API key:
```json
{
  "client_id": "12345678-1234-1234-1234-123456789012",
  "api_key": "your-generated-api-key-here"
}
```

**Important**: Save the API key - it's only shown once!

### 3. Configure User Management Service

Update `application.properties` if your Client Management Service runs on a different URL:

```properties
# Client Management Service Configuration
client-management.service.url=http://localhost:8080
client-management.service.timeout=5
```

### 4. Start User Management Service

```bash
./gradlew bootRun
```

The service will start on `http://localhost:8082`

## Usage

### Making Authenticated Requests

All requests to User Management Service now require the `x-api-key` header:

```bash
curl -X POST http://localhost:8082/user \
-H "Content-Type: application/json" \
-H "x-api-key: your-generated-api-key-here" \
-H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
-H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
-H "x-app-client-user-session-uuid: session-12345" \
-H "x-app-trace-id: trace-67890" \
-H "x-app-region-id: us-east-1" \
-d '{
  "username": "testuser",
  "firstName": "Test",
  "lastName": "User",
  "phoneInfo": {
    "number": "1234567890",
    "countryCode": "1",
    "verificationStatus": "verified"
  },
  "emailInfo": {
    "email": "test@example.com",
    "verificationStatus": "verified"
  }
}'
```

### Public Endpoints (No API Key Required)

These endpoints don't require authentication:
- `/actuator/**` - Health checks and metrics
- `/health/**` - Health endpoints
- `/swagger-ui/**` - API documentation
- `/v3/api-docs/**` - OpenAPI specs

## Error Responses

### Missing API Key
```json
{
  "error": "Missing API key",
  "message": "x-api-key header is required"
}
```

### Invalid API Key
```json
{
  "error": "Invalid API key", 
  "message": "The provided API key is invalid or expired"
}
```

## API Key Management

### Rotate API Key
```bash
curl -X POST http://localhost:8080/clients/{clientId}/api-keys \
-H "x-api-key: current-api-key"
```

### Revoke API Key
```bash
curl -X POST http://localhost:8080/clients/api-keys/{apiKeyId}/revoke \
-H "x-api-key: current-api-key"
```

## Environment Configuration

### Development
```properties
client-management.service.url=http://localhost:8080
```

### Production
```properties
client-management.service.url=https://your-client-management-service.com
client-management.service.timeout=10
```

## Troubleshooting

### Connection Issues
- Ensure Client Management Service is running
- Check network connectivity between services
- Verify the service URL configuration

### Authentication Failures
- Verify API key is valid and not expired
- Check API key format (no extra spaces/characters)
- Ensure Client Management Service is responding

### Logs
Check logs for detailed error information:
```bash
tail -f logs/user-management-service.log
```

## Security Considerations

1. **HTTPS**: Use HTTPS in production
2. **API Key Storage**: Store API keys securely
3. **Key Rotation**: Regularly rotate API keys
4. **Network Security**: Use VPC/private networks when possible
5. **Monitoring**: Monitor for suspicious API key usage
