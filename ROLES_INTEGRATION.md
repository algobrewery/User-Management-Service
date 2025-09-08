# Roles and Permissions Microservices Integration

This document describes how the User Management Service integrates with the Roles and Permissions Service as a separate microservice.

## Architecture Overview

The User Management Service now acts as a client to the Roles and Permissions Service, making HTTP calls to manage user roles and permissions. This follows a microservices architecture pattern where each service has a single responsibility.

```
┌─────────────────────┐    HTTP/REST    ┌─────────────────────────┐
│ User Management    │◄──────────────►│ Roles & Permissions    │
│ Service            │                 │ Service                 │
│ (Port 8080)       │                 │ (Port 8081)            │
└─────────────────────┘                 └─────────────────────────┘
```

## Service Integration

### 1. Roles Service Client

The `RolesServiceClient` interface defines all operations that can be performed with the Roles Service:

- **Role Management**: Create, read, update, delete roles
- **User Role Management**: Assign/remove roles to/from users
- **Permission Management**: Check user permissions

### 2. WebClient Configuration

The service uses Spring WebFlux's WebClient with:
- Configurable timeouts
- Proper error handling
- Reactive programming model

### 3. DTOs

All communication with the Roles Service uses dedicated DTOs in the `com.userapi.models.external.roles` package:
- `CreateRoleRequest` / `CreateRoleResponse`
- `AssignRoleRequest`
- `PermissionCheckRequest` / `PermissionCheckResponse`
- `RoleResponse`

## API Endpoints

### Roles Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/roles` | Create a new role |
| GET | `/api/v1/roles/{roleUuid}` | Get role by UUID |
| GET | `/api/v1/roles/organization/{orgUuid}` | Get organization roles |
| GET | `/api/v1/roles/system-managed` | Get system managed roles |
| PUT | `/api/v1/roles/{roleUuid}` | Update role |
| DELETE | `/api/v1/roles/{roleUuid}` | Delete role |

### User Role Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/roles/users/{userUuid}/assign` | Assign role to user |
| GET | `/api/v1/roles/users/{userUuid}` | Get user roles |
| DELETE | `/api/v1/roles/users/{userUuid}/roles/{roleUuid}` | Remove role from user |

### Permission Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/roles/permissions/check` | Check user permission |

## Configuration

### Application Properties

```properties
# Roles and Permissions Service Configuration
roles.service.url=https://i9vn73mmkg.execute-api.us-east-1.amazonaws.com/prod/
roles.service.timeout=10
```

### WebClient Beans

- `clientManagementWebClient`: For Client Management Service calls
- `rolesServiceWebClient`: For Roles and Permissions Service calls

## Default Roles

The service automatically creates default roles for new organizations:

### Admin Role
```json
{
  "version": "1.0",
  "data": {
    "read": ["*"],
    "write": ["*"],
    "delete": ["*"]
  },
  "features": {
    "execute": ["*"]
  }
}
```

### User Role
```json
{
  "version": "1.0",
  "data": {
    "read": ["users", "tasks", "clients", "organization"],
    "write": ["tasks"],
    "delete": []
  },
  "features": {
    "execute": ["create_task", "view_reports"]
  }
}
```

## Integration Service

The `UserRolesIntegrationService` provides high-level methods to:
- Create default roles for new organizations
- Assign appropriate roles to users
- Check user permissions
- Determine if a user is an admin

## Error Handling

- All service calls use reactive error handling
- Failed calls are logged with appropriate error messages
- Timeouts are configurable per service
- Circuit breaker pattern can be easily added

## Testing

### Unit Tests
- Mock the `RolesServiceClient` for testing business logic
- Test error scenarios and edge cases

### Integration Tests
- Test actual HTTP calls to the Roles Service
- Verify proper serialization/deserialization
- Test timeout and error scenarios

## Deployment

### Prerequisites
1. Roles and Permissions Service must be running and accessible
2. Network connectivity between services
3. Proper firewall rules for inter-service communication

### Environment Variables
```bash
ROLES_SERVICE_URL=http://roles-service:8081
ROLES_SERVICE_TIMEOUT=10
```

### Health Checks
The service includes health checks for the Roles Service dependency.

## Monitoring

### Metrics
- HTTP call success/failure rates
- Response times
- Timeout occurrences

### Logging
- All service calls are logged with appropriate levels
- Error scenarios include detailed error messages
- Performance metrics for debugging

## Future Enhancements

1. **Circuit Breaker**: Add resilience4j for fault tolerance
2. **Retry Logic**: Implement exponential backoff for failed calls
3. **Caching**: Cache frequently accessed role information
4. **Async Processing**: Use message queues for role assignments
5. **Audit Trail**: Log all role and permission changes

## Troubleshooting

### Common Issues

1. **Connection Refused**: Check if Roles Service is running
2. **Timeout Errors**: Verify network latency and service performance
3. **Serialization Errors**: Check DTO field mappings
4. **Permission Denied**: Verify user has appropriate roles

### Debug Steps

1. Check service logs for error messages
2. Verify network connectivity
3. Test Roles Service endpoints directly
4. Check configuration values
5. Monitor WebClient metrics
