# API Testing Guide - User Management Service with Roles Integration

## Prerequisites

1. **Start Roles & Permissions Service** (Port 8080)
2. **Start User Management Service** (Port 8081)
3. **Ensure PostgreSQL and Redis are running** for the Roles service

## Base URLs

- **User Management Service**: `http://localhost:8081`
- **Roles & Permissions Service**: `http://localhost:8080`

---

## 1. Health Check Endpoints

### Check Roles Service Health
```bash
curl -X GET "http://localhost:8080/actuator/health" \
  -H "Content-Type: application/json"
```

### Check User Management Service Health
```bash
curl -X GET "http://localhost:8081/actuator/health" \
  -H "Content-Type: application/json"
```

---

## 2. System Roles Endpoints

### Get System-Managed Roles
```bash
curl -X GET "http://localhost:8081/role/system-managed" \
  -H "Content-Type: application/json"
```

**Expected Response**: List of default roles (Owner, Manager, User, Operator)

---

## 3. Custom Role Management

### Create a Custom Role
```bash
curl -X POST "http://localhost:8081/role" \
  -H "Content-Type: application/json" \
  -H "x-app-user-uuid: admin-user-123" \
  -H "x-app-org-uuid: test-org-456" \
  -d '{
    "role_name": "Project Manager",
    "description": "Can manage projects and tasks within organization",
    "organization_uuid": "test-org-456",
    "role_management_type": "CUSTOMER_MANAGED",
    "policy": {
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
  }'
```

### Get Role by UUID
```bash
curl -X GET "http://localhost:8081/role/{ROLE_UUID}" \
  -H "Content-Type: application/json" \
  -H "x-app-org-uuid: test-org-456"
```

### Get Organization Roles
```bash
curl -X GET "http://localhost:8081/role/organization/test-org-456" \
  -H "Content-Type: application/json"
```

### Update Role
```bash
curl -X PUT "http://localhost:8081/role/{ROLE_UUID}" \
  -H "Content-Type: application/json" \
  -H "x-app-user-uuid: admin-user-123" \
  -H "x-app-org-uuid: test-org-456" \
  -d '{
    "role_name": "Senior Project Manager",
    "description": "Enhanced project management with client access",
    "organization_uuid": "test-org-456",
    "role_management_type": "CUSTOMER_MANAGED",
    "policy": {
      "version": "1.0",
      "data": {
        "read": ["users", "tasks", "clients", "organization"],
        "write": ["tasks", "clients"],
        "delete": ["tasks"]
      },
      "features": {
        "execute": ["create_task", "assign_task", "generate_reports"]
      }
    }
  }'
```

### Delete Role
```bash
curl -X DELETE "http://localhost:8081/role/{ROLE_UUID}" \
  -H "Content-Type: application/json" \
  -H "x-app-user-uuid: admin-user-123" \
  -H "x-app-org-uuid: test-org-456"
```

---

## 4. User Role Assignment

### Assign Role to User
```bash
curl -X POST "http://localhost:8081/role/user/test-user-123/assign" \
  -H "Content-Type: application/json" \
  -H "x-app-user-uuid: admin-user-123" \
  -H "x-app-org-uuid: test-org-456" \
  -d '{
    "role_uuid": "{ROLE_UUID}",
    "organization_uuid": "test-org-456"
  }'
```

### Get User Roles
```bash
curl -X GET "http://localhost:8081/role/user/test-user-123" \
  -H "Content-Type: application/json" \
  -H "x-app-org-uuid: test-org-456"
```

### Remove Role from User
```bash
curl -X DELETE "http://localhost:8081/role/user/test-user-123/roles/{ROLE_UUID}" \
  -H "Content-Type: application/json" \
  -H "x-app-org-uuid: test-org-456"
```

### Assign Admin Role to Existing User
```bash
curl -X POST "http://localhost:8081/role/user/test-user-123/assign-admin" \
  -H "Content-Type: application/json" \
  -H "x-app-user-uuid: admin-user-123" \
  -H "x-app-org-uuid: test-org-456"
```

### Assign Specific Role by UUID
```bash
curl -X POST "http://localhost:8081/role/user/test-user-123/assign-role/{ROLE_UUID}" \
  -H "Content-Type: application/json" \
  -H "x-app-user-uuid: admin-user-123" \
  -H "x-app-org-uuid: test-org-456"
```

---

## 5. Permission Checking

### Check User Permission
```bash
curl -X POST "http://localhost:8081/role/permissions/check" \
  -H "Content-Type: application/json" \
  -d '{
    "user_uuid": "test-user-123",
    "organization_uuid": "test-org-456",
    "resource": "tasks",
    "action": "read",
    "resource_id": "specific-task-id"
  }'
```

**Expected Response**:
```json
{
  "has_permission": true,
  "role_uuid": "role-uuid-here",
  "role_name": "Project Manager",
  "granted_scope": "team"
}
```

### Test Permission Denial
```bash
curl -X POST "http://localhost:8081/role/permissions/check" \
  -H "Content-Type: application/json" \
  -d '{
    "user_uuid": "test-user-123",
    "organization_uuid": "test-org-456",
    "resource": "users",
    "action": "delete",
    "resource_id": "specific-user-id"
  }'
```

---

## 6. Complete Testing Workflow

### Step 1: Health Checks
```bash
# Check both services are running
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
```

### Step 2: Get System Roles
```bash
curl -X GET "http://localhost:8081/role/system-managed"
```

### Step 3: Create Custom Role
```bash
curl -X POST "http://localhost:8081/role" \
  -H "Content-Type: application/json" \
  -H "x-app-user-uuid: admin-user-123" \
  -H "x-app-org-uuid: test-org-456" \
  -d '{
    "role_name": "Test Manager",
    "description": "Test role for API testing",
    "organization_uuid": "test-org-456",
    "role_management_type": "CUSTOMER_MANAGED",
    "policy": {
      "version": "1.0",
      "data": {
        "read": ["users", "tasks"],
        "write": ["tasks"],
        "delete": []
      },
      "features": {
        "execute": ["create_task"]
      }
    }
  }'
```

### Step 4: Assign Role to User
```bash
# Use the role_uuid from Step 3 response
curl -X POST "http://localhost:8081/role/user/test-user-123/assign" \
  -H "Content-Type: application/json" \
  -H "x-app-user-uuid: admin-user-123" \
  -H "x-app-org-uuid: test-org-456" \
  -d '{
    "role_uuid": "{ROLE_UUID_FROM_STEP_3}",
    "organization_uuid": "test-org-456"
  }'
```

### Step 5: Test Permissions
```bash
# Should PASS - User has read access to tasks
curl -X POST "http://localhost:8081/role/permissions/check" \
  -H "Content-Type: application/json" \
  -d '{
    "user_uuid": "test-user-123",
    "organization_uuid": "test-org-456",
    "resource": "tasks",
    "action": "read"
  }'

# Should FAIL - User doesn't have delete access
curl -X POST "http://localhost:8081/role/permissions/check" \
  -H "Content-Type: application/json" \
  -d '{
    "user_uuid": "test-user-123",
    "organization_uuid": "test-org-456",
    "resource": "users",
    "action": "delete"
  }'
```

### Step 6: Get User Roles
```bash
curl -X GET "http://localhost:8081/role/user/test-user-123" \
  -H "x-app-org-uuid: test-org-456"
```

---

## 7. Policy Examples for Testing

### Admin Policy (Full Access)
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

### Manager Policy (Limited Access)
```json
{
  "version": "1.0",
  "data": {
    "read": ["users", "tasks", "clients", "organization"],
    "write": ["tasks", "clients"],
    "delete": ["tasks"]
  },
  "features": {
    "execute": ["create_task", "assign_task", "generate_reports"]
  }
}
```

### Read-Only Policy
```json
{
  "version": "1.0",
  "data": {
    "read": ["users", "tasks", "clients"]
  },
  "features": {
    "execute": []
  }
}
```

---

## 8. Testing with Postman

### Environment Variables
Create these variables in Postman:
```json
{
  "baseUrl": "http://localhost:8081",
  "rolesServiceUrl": "http://localhost:8080",
  "userUuid": "test-user-123",
  "adminUuid": "admin-user-456",
  "organizationUuid": "test-org-789",
  "roleUuid": "{{$guid}}"
}
```

### Collection Structure
1. **Health Checks**
2. **System Roles**
3. **Custom Role CRUD**
4. **User Role Assignment**
5. **Permission Testing**

---

## 9. Expected Responses

### Successful Role Creation
```json
{
  "role_uuid": "550e8400-e29b-41d4-a716-446655440000",
  "role_name": "Project Manager",
  "description": "Can manage projects and tasks",
  "organization_uuid": "test-org-456",
  "role_management_type": "CUSTOMER_MANAGED",
  "policy": {...},
  "created_at": "2025-01-03T10:00:00Z",
  "created_by": "admin-user-123"
}
```

### Permission Check Success
```json
{
  "has_permission": true,
  "role_uuid": "550e8400-e29b-41d4-a716-446655440000",
  "role_name": "Project Manager",
  "granted_scope": "team"
}
```

### Permission Check Failure
```json
{
  "has_permission": false,
  "role_uuid": null,
  "role_name": null,
  "granted_scope": null
}
```

---

## 10. Troubleshooting

### Common Issues

1. **Connection Refused**
   ```bash
   # Check if services are running
   netstat -an | findstr :8080
   netstat -an | findstr :8081
   ```

2. **Permission Always False**
   - Verify user has assigned roles
   - Check policy format
   - Ensure organization_uuid matches

3. **Role Creation Fails**
   - Check required headers
   - Verify policy JSON structure
   - Ensure role_management_type is valid

### Debug Commands
```bash
# Check service logs
tail -f logs/user-management-service.log

# Test direct roles service
curl http://localhost:8080/role/system-managed

# Verify database connection
curl http://localhost:8080/actuator/health
```

---

## Quick Test Script

Save this as `test-integration.bat`:

```batch
@echo off
echo Testing User Management Service Integration...

echo.
echo 1. Health Check - User Management Service
curl -s http://localhost:8081/actuator/health

echo.
echo 2. Health Check - Roles Service
curl -s http://localhost:8080/actuator/health

echo.
echo 3. Get System Roles
curl -s -X GET "http://localhost:8081/role/system-managed"

echo.
echo Integration test complete!
```

Run with: `test-integration.bat`