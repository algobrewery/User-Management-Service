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

| Method | Endpoint | Description | Status |
|--------|----------|-------------|---------|
| POST | `/api/v1/roles` | Create a new role | ✅ Active |
| GET | `/api/v1/roles/{roleUuid}` | Get role by UUID | ✅ Active |
| **POST** | **`/api/v1/roles/search`** | **Search and filter roles (NEW)** | ✅ **Recommended** |
| GET | `/api/v1/roles/organization/{orgUuid}` | Get organization roles | ❌ Removed |
| GET | `/api/v1/roles/system-managed` | Get system managed roles | ❌ Removed |
| GET | `/api/v1/roles/user/{userUuid}` | Get user roles | ❌ Removed |
| PUT | `/api/v1/roles/{roleUuid}` | Update role | ✅ Active |
| DELETE | `/api/v1/roles/{roleUuid}` | Delete role | ✅ Active |

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

## New Search Endpoint

### POST `/api/v1/roles/search`

The new unified search endpoint replaces the separate organization and system-managed role endpoints with a flexible filtering system.

#### Request Body:
```json
{
  "filterCriteria": {
    "attributes": [
      {
        "name": "role_management_type",
        "values": ["SYSTEM_MANAGED"]
      },
      {
        "name": "role_name",
        "values": ["Admin", "User"]
      }
    ]
  },
  "page": 0,
  "size": 10,
  "sortBy": "roleName",
  "sortDirection": "asc"
}
```

#### Response:
```json
{
  "roles": [...],
  "totalElements": 25,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 10,
  "hasNext": true,
  "hasPrevious": false
}
```

#### Migration Guide:

**Old way:**
```bash
GET /role/organization/{orgUuid}
GET /role/system-managed
```

**New way:**
```bash
# Get organization roles
POST /role/search
{
  "page": 0,
  "size": 10
}

# Get system-managed roles (global roles available to all organizations)
POST /role/search
{
  "filterCriteria": {
    "attributes": [
      {
        "name": "role_management_type",
        "values": ["SYSTEM_MANAGED"]
      }
    ]
  }
}

# Get customer-managed roles for specific organization
POST /role/search
{
  "filterCriteria": {
    "attributes": [
      {
        "name": "role_management_type",
        "values": ["CUSTOMER_MANAGED"]
      },
      {
        "name": "organization_uuid",
        "values": ["cts"]
      }
    ]
  }
}

# Get all roles for an organization (default behavior)
POST /role/search
{
  "filterCriteria": {
    "attributes": [
      {
        "name": "organization_uuid",
        "values": ["cts"]
      }
    ]
  }
}

# Get roles assigned to a specific user
POST /role/search
{
  "filterCriteria": {
    "attributes": [
      {
        "name": "user_uuid",
        "values": ["42388507-ec8f-47ef-a7c7-8ddb69763ac6"]
      }
    ]
  }
}
```

## Migration Guide

### From Legacy Endpoints to Unified Search

#### 1. **Organization Roles**
```http
# OLD (Deprecated)
GET /role/organization/{organizationUuid}

# NEW (Recommended)
POST /role/search
{
  "filterCriteria": {
    "attributes": [
      {
        "name": "role_management_type",
        "values": ["CUSTOMER_MANAGED"]
      },
      {
        "name": "organization_uuid",
        "values": ["{organizationUuid}"]
      }
    ]
  }
}
```

#### 2. **System-Managed Roles**
```http
# OLD (Deprecated)
GET /role/system-managed

# NEW (Recommended)
POST /role/search
{
  "filterCriteria": {
    "attributes": [
      {
        "name": "role_management_type",
        "values": ["SYSTEM_MANAGED"]
      }
    ]
  }
}
```

#### 3. **User Roles**
```http
# OLD (Deprecated)
GET /role/user/{userUuid}

# NEW (Recommended)
POST /role/search
{
  "filterCriteria": {
    "attributes": [
      {
        "name": "user_uuid",
        "values": ["{userUuid}"]
      }
    ]
  }
}
```

**✅ User Roles Search**: The search API now supports user role queries using the legacy `getUserRoles` method. When filtering by `user_uuid`, the system will:
1. Call the external service's user roles endpoint directly
2. Return the roles assigned to the specified user
3. Handle errors gracefully by returning empty list if the endpoint fails
4. This approach uses the original method that was working before

### Current Limitations

| Feature | Status | Notes |
|---------|--------|-------|
| **User Role Queries** | ✅ Supported (Alternative) | Uses permission-based filtering |
| **Organization Role Queries** | ✅ Supported | Works correctly |
| **System-Managed Role Queries** | ✅ Supported | Works correctly |
| **Role Assignment** | ✅ Supported | Can assign roles to users |
| **Role Creation/Update/Delete** | ✅ Supported | Full CRUD operations |

### How User Role Queries Work

The user role search uses the legacy `getUserRoles` method:

1. **Direct API Call**: Calls the external service's user roles endpoint directly
2. **Role Assignment**: Returns roles that are actually assigned to the user
3. **Error Handling**: If the external service fails, returns empty list gracefully
4. **Simple Approach**: Uses the original method that was working before the changes
5. **Reliable**: Leverages the existing working endpoint instead of complex workarounds

## Role Update Restrictions

### ✅ **ALLOWED to Update:**

| Field | Description | Example |
|-------|-------------|---------|
| **`description`** | Role description | "Updated role description" |
| **`policy`** | Permissions and features | Update JSON policy object |
| **`is_active`** | Enable/disable role | `true` or `false` |

### ❌ **NOT ALLOWED to Update:**

| Field | Reason | Impact if Changed |
|-------|--------|-------------------|
| **`role_name`** | Unique identifier | Breaks role references |
| **`role_uuid`** | System-generated ID | Breaks all relationships |
| **`organization_uuid`** | Ownership boundary | Security risk |
| **`role_management_type`** | System vs Customer scope | Changes role scope |
| **`created_at`** | Audit trail | Breaks audit history |
| **`created_by`** | Audit trail | Breaks audit history |
| **`updated_at`** | System-managed | Auto-generated timestamp |

### Example Update Request:

```json
PUT /role/{roleUuid}
{
  "description": "Updated role description",
  "policy": {
    "data": {
      "read": ["users", "roles"],
      "write": ["users"]
    },
    "features": {
      "execute": ["view_reports"]
    }
  },
  "is_active": true
}
```

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
