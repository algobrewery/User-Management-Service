# User Management Service - Roles & Permissions Integration Setup

## Overview

Your User Management Service has been successfully integrated with the Roles and Permissions Service from https://github.com/algobrewery/Roles-and-Permissions. This document outlines the integration setup and how to use it.

## What Was Updated

### 1. Configuration Changes
- **File**: `src/main/resources/application.properties`
- **Change**: Updated `roles.service.url=http://localhost:8080` to match the GitHub service default port
- **Note**: The GitHub service runs on port 8080 by default, while your user management service should run on a different port (e.g., 8081)

### 2. API Client Updates
- **File**: `src/main/java/com/userapi/service/impl/RolesServiceClientImpl.java`
- **Changes**:
  - Removed unnecessary `x-app-org-uuid` headers where not needed
  - Updated endpoint mappings to match GitHub service structure
  - Fixed user roles endpoint to use query parameters: `/user/{userUuid}/roles?organization_uuid={organizationUuid}`

### 3. DTO Updates
- **CreateRoleRequest**: Updated to use `role_name` field and `JsonNode` for policy
- **RoleResponse**: Updated to match GitHub service response structure with proper JSON property mappings
- **AssignRoleRequest**: Already matched the required structure

## Setup Instructions

### 1. Start the Roles & Permissions Service

First, clone and start the GitHub service:

```bash
# Clone the repository
git clone https://github.com/algobrewery/Roles-and-Permissions.git
cd Roles-and-Permissions

# Configure database (update src/main/resources/application.yml)
# Make sure PostgreSQL and Redis are running

# Start the service (runs on port 8080)
./gradlew bootRun
```

### 2. Configure Your User Management Service

Update your `application.properties` if needed:

```properties
# Your service should run on a different port
server.port=8081

# Roles service configuration (already updated)
roles.service.url=http://localhost:8080
roles.service.timeout=10
```

### 3. Start Your User Management Service

```bash
# From your user management service directory
./gradlew bootRun
```

## API Usage Examples

### 1. Create a Custom Role

```bash
curl -X POST "http://localhost:8081/role" \
  -H "Content-Type: application/json" \
  -H "x-app-user-uuid: admin-user-123" \
  -H "x-app-org-uuid: test-org-456" \
  -d '{
    "role_name": "Project Manager",
    "description": "Can manage projects and tasks",
    "organization_uuid": "test-org-456",
    "role_management_type": "CUSTOMER_MANAGED",
    "policy": {
      "version": "1.0",
      "data": {
        "read": ["users", "tasks", "clients"],
        "write": ["tasks"]
      },
      "features": {
        "execute": ["create_task", "assign_task"]
      }
    }
  }'
```

### 2. Assign Role to User

```bash
curl -X POST "http://localhost:8081/role/user/test-user-123/assign" \
  -H "Content-Type: application/json" \
  -H "x-app-user-uuid: admin-user-123" \
  -H "x-app-org-uuid: test-org-456" \
  -d '{
    "role_uuid": "created-role-uuid-here",
    "organization_uuid": "test-org-456"
  }'
```

### 3. Check User Permissions

```bash
curl -X POST "http://localhost:8081/role/permissions/check" \
  -H "Content-Type: application/json" \
  -d '{
    "user_uuid": "test-user-123",
    "organization_uuid": "test-org-456",
    "resource": "tasks",
    "action": "read"
  }'
```

### 4. Get User Roles

```bash
curl -X GET "http://localhost:8081/role/user/test-user-123" \
  -H "x-app-org-uuid: test-org-456"
```

## Policy Structure

The GitHub service expects policies in this specific format:

```json
{
  "version": "1.0",
  "data": {
    "read": ["users", "tasks", "clients"],
    "write": ["tasks"],
    "delete": ["tasks"]
  },
  "features": {
    "execute": ["create_task", "assign_task", "generate_reports"]
  }
}
```

### Available Resources
- `users` - User profile information
- `tasks` - Task management data
- `clients` - Client/customer information
- `organization` - Organization settings
- `roles` - Role management data
- `*` - Wildcard for all resources

### Available Actions
- `read` - Read access to resources
- `write` - Modify access to resources
- `delete` - Delete access to resources
- `execute` - Execute specific features

## System-Managed Roles

The GitHub service provides these default roles:

1. **Owner** - Full access to all operations
2. **Manager** - Can view/edit users, view organization, approve requests
3. **User** - Can view and edit own profile only
4. **Operator** - System operations and monitoring capabilities

## Integration Features

### 1. Default Role Creation
Your service automatically creates default roles for new organizations using the `UserRolesIntegrationService`.

### 2. Permission Checking
Use the `RolesServiceClient.hasPermission()` method for quick permission checks in your business logic.

### 3. Error Handling
All service calls include proper error handling with detailed logging.

### 4. Reactive Programming
The integration uses Spring WebFlux for non-blocking, reactive communication.

## Troubleshooting

### Common Issues

1. **Connection Refused**
   - Ensure the Roles & Permissions service is running on port 8080
   - Check network connectivity between services

2. **Permission Always Returns False**
   - Verify policy format uses `data`/`features` structure
   - Ensure user has assigned roles in the organization
   - Check that action matches policy keys

3. **Role Creation Fails**
   - Ensure `role_management_type` is `CUSTOMER_MANAGED` or `SYSTEM_MANAGED`
   - Verify policy is valid JSON object
   - Check `x-app-user-uuid` header is present

4. **Serialization Errors**
   - Verify DTO field mappings match the GitHub service
   - Check JSON property annotations

### Debug Steps

1. Check service logs for error messages
2. Verify network connectivity: `curl http://localhost:8080/actuator/health`
3. Test Roles Service endpoints directly
4. Monitor WebClient metrics

## Next Steps

1. **Testing**: Use the provided API examples to test the integration
2. **Monitoring**: Set up health checks and metrics collection
3. **Security**: Configure proper authentication between services
4. **Scaling**: Consider adding circuit breakers and retry logic for production

## Support

For issues with the Roles & Permissions service, refer to:
- GitHub Repository: https://github.com/algobrewery/Roles-and-Permissions
- Service Documentation: README.md in the repository

For integration issues, check the logs in both services and verify the configuration matches this guide.