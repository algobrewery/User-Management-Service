# User Management Service - Postman Testing Guide

## Overview
This guide provides comprehensive testing instructions for all endpoints in the User Management Service. The service handles user operations, role management, and user-role assignments with proper authorization and validation.

## Base URL
```
http://localhost:8080
```

## Required Headers (for most endpoints)
```
x-app-org-uuid: your-organization-uuid
x-app-user-uuid: your-user-uuid
x-app-client-user-session-uuid: your-session-uuid
x-app-trace-id: unique-trace-id
x-app-region-id: your-region-id
Content-Type: application/json
```

---

## Testing Order & Endpoints

### Phase 1: Bootstrap Setup (No Authorization Required)
*These endpoints are used for initial system setup and don't require authentication.*

#### 1.1 Create System Managed Roles (Bootstrap)
**Purpose**: Create default system roles for the organization
**Business Value**: Establishes the foundational role structure for the organization

**Request:**
```http
POST /role/bootstrap/create
Content-Type: application/json

{
  "role_name": "System Administrator",
  "description": "Full system access role",
  "organization_uuid": "org-123e4567-e89b-12d3-a456-426614174000",
  "role_management_type": "SYSTEM_MANAGED",
  "policy": {
    "permissions": [
      {
        "resource": "*",
        "actions": ["*"]
      }
    ]
  }
}
```

**Expected Response (201 Created):**
```json
{
  "role_uuid": "role-123e4567-e89b-12d3-a456-426614174000",
  "role_name": "System Administrator",
  "description": "Full system access role",
  "organization_uuid": "org-123e4567-e89b-12d3-a456-426614174000",
  "role_management_type": "SYSTEM_MANAGED",
  "policy": {
    "permissions": [
      {
        "resource": "*",
        "actions": ["*"]
      }
    ]
  },
  "created_at": "2024-01-15T10:30:00Z",
  "created_by": "system"
}
```

#### 1.2 Get System Managed Roles (Bootstrap)
**Purpose**: Retrieve all system-managed roles
**Business Value**: Verify system roles are properly configured

**Request:**
```http
GET /role/bootstrap/system-managed
```

**Expected Response (200 OK):**
```json
[
  {
    "role_uuid": "role-123e4567-e89b-12d3-a456-426614174000",
    "role_name": "System Administrator",
    "description": "Full system access role",
    "organization_uuid": "org-123e4567-e89b-12d3-a456-426614174000",
    "role_management_type": "SYSTEM_MANAGED",
    "policy": {
      "permissions": [
        {
          "resource": "*",
          "actions": ["*"]
        }
      ]
    },
    "created_at": "2024-01-15T10:30:00Z",
    "created_by": "system"
  }
]
```

#### 1.3 Bootstrap Organization Setup
**Purpose**: Set up default roles and assign admin role to a user
**Business Value**: Complete organization initialization with proper admin access

**Request:**
```http
POST /role/bootstrap/organization/org-123e4567-e89b-12d3-a456-426614174000/setup?adminUserUuid=user-123e4567-e89b-12d3-a456-426614174000
```

**Expected Response (200 OK):**
```json
"Organization setup complete. Admin role created and assigned to user: user-123e4567-e89b-12d3-a456-426614174000"
```

#### 1.4 Bootstrap Assign Admin Role
**Purpose**: Assign admin role to an existing user
**Business Value**: Grant administrative privileges to a specific user

**Request:**
```http
POST /role/bootstrap/user/user-123e4567-e89b-12d3-a456-426614174000/assign-admin?organizationUuid=org-123e4567-e89b-12d3-a456-426614174000&assignedBy=system
```

**Expected Response (200 OK):**
```json
"Admin role assigned successfully to user: user-123e4567-e89b-12d3-a456-426614174000"
```

---

### Phase 2: User Management Operations

#### 2.1 Create User
**Purpose**: Create a new user in the system
**Business Value**: Onboard new employees with complete profile information
**Authorization**: Requires 'USER' 'CREATE' permission

**Request:**
```http
POST /user
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
x-app-user-uuid: user-123e4567-e89b-12d3-a456-426614174000
x-app-client-user-session-uuid: session-123e4567-e89b-12d3-a456-426614174000
x-app-trace-id: trace-123e4567-e89b-12d3-a456-426614174000
x-app-region-id: us-east-1
Content-Type: application/json

{
  "username": "john.doe",
  "firstName": "John",
  "middleName": "Michael",
  "lastName": "Doe",
  "phoneInfo": {
    "number": "1234567890",
    "countryCode": 1,
    "verificationStatus": "VERIFIED"
  },
  "emailInfo": {
    "email": "john.doe@company.com",
    "verificationStatus": "VERIFIED"
  },
  "employmentInfoList": [
    {
      "startDate": "2024-01-15T09:00:00",
      "endDate": null,
      "jobTitle": "Software Engineer",
      "organizationUnit": "Engineering",
      "reportingManager": "manager-123e4567-e89b-12d3-a456-426614174000",
      "extensionsData": {
        "department": "Product Development",
        "location": "New York",
        "employeeId": "EMP001"
      }
    }
  ]
}
```

**Expected Response (201 Created):**
```json
{
  "userId": "user-456e7890-e89b-12d3-a456-426614174000",
  "username": "john.doe",
  "status": "ACTIVE",
  "message": "User created successfully",
  "httpStatus": "CREATED",
  "result": "SUCCESS",
  "reasonCode": "USER_CREATED"
}
```

#### 2.2 Get User by ID
**Purpose**: Retrieve detailed information about a specific user
**Business Value**: Access complete user profile for management and HR operations
**Authorization**: Requires 'USER' 'READ' permission

**Request:**
```http
GET /user/user-456e7890-e89b-12d3-a456-426614174000
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
x-app-user-uuid: user-123e4567-e89b-12d3-a456-426614174000
x-app-client-user-session-uuid: session-123e4567-e89b-12d3-a456-426614174000
x-app-trace-id: trace-123e4567-e89b-12d3-a456-426614174000
x-app-region-id: us-east-1
```

**Expected Response (200 OK):**
```json
{
  "userId": "user-456e7890-e89b-12d3-a456-426614174000",
  "username": "john.doe",
  "firstName": "John",
  "middleName": "Michael",
  "lastName": "Doe",
  "email": "john.doe@company.com",
  "phone": "+11234567890",
  "startDate": "2024-01-15T09:00:00",
  "endDate": null,
  "status": "ACTIVE",
  "jobProfiles": [
    {
      "jobTitle": "Software Engineer",
      "organizationUnit": "Engineering",
      "reportingManager": "manager-123e4567-e89b-12d3-a456-426614174000",
      "startDate": "2024-01-15T09:00:00",
      "endDate": null
    }
  ],
  "httpStatus": "OK",
  "result": "SUCCESS"
}
```

#### 2.3 Update User
**Purpose**: Modify existing user information
**Business Value**: Keep user profiles current with role changes, contact updates, etc.
**Authorization**: Requires 'USER' 'UPDATE' permission

**Request:**
```http
PUT /user/user-456e7890-e89b-12d3-a456-426614174000
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
x-app-user-uuid: user-123e4567-e89b-12d3-a456-426614174000
x-app-client-user-session-uuid: session-123e4567-e89b-12d3-a456-426614174000
x-app-trace-id: trace-123e4567-e89b-12d3-a456-426614174000
x-app-region-id: us-east-1
Content-Type: application/json

{
  "firstName": "John",
  "middleName": "Michael",
  "lastName": "Doe",
  "status": "ACTIVE",
  "phoneInfo": {
    "number": "1234567890",
    "countryCode": 1,
    "verificationStatus": "VERIFIED"
  },
  "emailInfo": {
    "email": "john.doe.updated@company.com",
    "verificationStatus": "VERIFIED"
  },
  "employmentInfoList": [
    {
      "startDate": "2024-01-15T09:00:00",
      "endDate": null,
      "jobTitle": "Senior Software Engineer",
      "organizationUnit": "Engineering",
      "reportingManager": "manager-123e4567-e89b-12d3-a456-426614174000",
      "extensionsData": {
        "department": "Product Development",
        "location": "New York",
        "employeeId": "EMP001",
        "promotionDate": "2024-06-01T09:00:00"
      }
    }
  ]
}
```

**Expected Response (200 OK):**
```json
{
  "userId": "user-456e7890-e89b-12d3-a456-426614174000",
  "username": "john.doe",
  "status": "ACTIVE",
  "message": "User updated successfully",
  "httpStatus": "OK",
  "result": "SUCCESS"
}
```

#### 2.4 Deactivate User
**Purpose**: Deactivate a user account (soft delete)
**Business Value**: Handle employee departures while maintaining data integrity
**Authorization**: Requires 'USER' 'DELETE' permission

**Request:**
```http
DELETE /user/user-456e7890-e89b-12d3-a456-426614174000
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
```

**Expected Response (200 OK):**
```json
{
  "userId": "user-456e7890-e89b-12d3-a456-426614174000",
  "username": "john.doe",
  "status": "INACTIVE",
  "message": "User deactivated successfully",
  "httpStatus": "OK",
  "result": "SUCCESS"
}
```

#### 2.5 List Users with Filter
**Purpose**: Search and filter users with pagination
**Business Value**: Efficient user management and reporting capabilities
**Authorization**: Requires 'USER' 'READ' permission

**Request:**
```http
POST /users/filter
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
x-app-user-uuid: user-123e4567-e89b-12d3-a456-426614174000
x-app-client-user-session-uuid: session-123e4567-e89b-12d3-a456-426614174000
x-app-trace-id: trace-123e4567-e89b-12d3-a456-426614174000
x-app-region-id: us-east-1
Content-Type: application/json

{
  "filterCriteria": {
    "status": "ACTIVE",
    "organizationUnit": "Engineering"
  },
  "selector": {
    "fields": ["userId", "username", "firstName", "lastName", "email", "status"]
  },
  "page": 0,
  "size": 10,
  "sortBy": "firstName",
  "sortDirection": "asc"
}
```

**Expected Response (200 OK):**
```json
{
  "users": [
    {
      "userId": "user-456e7890-e89b-12d3-a456-426614174000",
      "username": "john.doe",
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@company.com",
      "status": "ACTIVE"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 10,
  "hasNext": false,
  "hasPrevious": false
}
```

#### 2.6 Get User Hierarchy
**Purpose**: Retrieve organizational hierarchy for a specific user
**Business Value**: Understand reporting structure and organizational relationships
**Authorization**: Requires 'USER' 'READ' permission

**Request:**
```http
GET /users/user-456e7890-e89b-12d3-a456-426614174000/hierarchy
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
```

**Expected Response (200 OK):**
```json
{
  "userId": "user-456e7890-e89b-12d3-a456-426614174000",
  "username": "john.doe",
  "reportingManager": {
    "userId": "manager-123e4567-e89b-12d3-a456-426614174000",
    "username": "jane.smith",
    "firstName": "Jane",
    "lastName": "Smith"
  },
  "directReports": [],
  "organizationUnit": "Engineering",
  "jobTitle": "Senior Software Engineer"
}
```

---

### Phase 3: Role Management Operations

#### 3.1 Create Role
**Purpose**: Create a new role with specific permissions
**Business Value**: Define access control policies for different organizational functions
**Authorization**: Requires 'ROLE' 'CREATE' permission

**Request:**
```http
POST /role
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
x-app-user-uuid: user-123e4567-e89b-12d3-a456-426614174000
Content-Type: application/json

{
  "role_name": "Project Manager",
  "description": "Role for managing projects and team members",
  "organization_uuid": "org-123e4567-e89b-12d3-a456-426614174000",
  "role_management_type": "ORGANIZATION_MANAGED",
  "policy": {
    "permissions": [
      {
        "resource": "PROJECT",
        "actions": ["CREATE", "READ", "UPDATE", "DELETE"]
      },
      {
        "resource": "USER",
        "actions": ["READ"]
      },
      {
        "resource": "TASK",
        "actions": ["CREATE", "READ", "UPDATE", "DELETE"]
      }
    ]
  }
}
```

**Expected Response (201 Created):**
```json
{
  "role_uuid": "role-789e0123-e89b-12d3-a456-426614174000",
  "role_name": "Project Manager",
  "description": "Role for managing projects and team members",
  "organization_uuid": "org-123e4567-e89b-12d3-a456-426614174000",
  "role_management_type": "ORGANIZATION_MANAGED",
  "policy": {
    "permissions": [
      {
        "resource": "PROJECT",
        "actions": ["CREATE", "READ", "UPDATE", "DELETE"]
      },
      {
        "resource": "USER",
        "actions": ["READ"]
      },
      {
        "resource": "TASK",
        "actions": ["CREATE", "READ", "UPDATE", "DELETE"]
      }
    ]
  },
  "created_at": "2024-01-15T11:00:00Z",
  "created_by": "user-123e4567-e89b-12d3-a456-426614174000"
}
```

#### 3.2 Get Role by UUID
**Purpose**: Retrieve detailed information about a specific role
**Business Value**: Review role permissions and configuration
**Authorization**: No specific permission required

**Request:**
```http
GET /role/role-789e0123-e89b-12d3-a456-426614174000
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
```

**Expected Response (200 OK):**
```json
{
  "role_uuid": "role-789e0123-e89b-12d3-a456-426614174000",
  "role_name": "Project Manager",
  "description": "Role for managing projects and team members",
  "organization_uuid": "org-123e4567-e89b-12d3-a456-426614174000",
  "role_management_type": "ORGANIZATION_MANAGED",
  "policy": {
    "permissions": [
      {
        "resource": "PROJECT",
        "actions": ["CREATE", "READ", "UPDATE", "DELETE"]
      },
      {
        "resource": "USER",
        "actions": ["READ"]
      },
      {
        "resource": "TASK",
        "actions": ["CREATE", "READ", "UPDATE", "DELETE"]
      }
    ]
  },
  "created_at": "2024-01-15T11:00:00Z",
  "updated_at": "2024-01-15T11:00:00Z",
  "created_by": "user-123e4567-e89b-12d3-a456-426614174000"
}
```

#### 3.3 Get Organization Roles
**Purpose**: Retrieve all roles for a specific organization
**Business Value**: Overview of all available roles in the organization
**Authorization**: No specific permission required

**Request:**
```http
GET /role/organization/org-123e4567-e89b-12d3-a456-426614174000
```

**Expected Response (200 OK):**
```json
[
  {
    "role_uuid": "role-123e4567-e89b-12d3-a456-426614174000",
    "role_name": "System Administrator",
    "description": "Full system access role",
    "organization_uuid": "org-123e4567-e89b-12d3-a456-426614174000",
    "role_management_type": "SYSTEM_MANAGED",
    "created_at": "2024-01-15T10:30:00Z",
    "created_by": "system"
  },
  {
    "role_uuid": "role-789e0123-e89b-12d3-a456-426614174000",
    "role_name": "Project Manager",
    "description": "Role for managing projects and team members",
    "organization_uuid": "org-123e4567-e89b-12d3-a456-426614174000",
    "role_management_type": "ORGANIZATION_MANAGED",
    "created_at": "2024-01-15T11:00:00Z",
    "created_by": "user-123e4567-e89b-12d3-a456-426614174000"
  }
]
```

#### 3.4 Get System Managed Roles
**Purpose**: Retrieve all system-managed roles
**Business Value**: Access to core system roles that cannot be modified
**Authorization**: Requires 'SYSTEM_ROLE' 'READ' permission

**Request:**
```http
GET /role/system-managed
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
x-app-user-uuid: user-123e4567-e89b-12d3-a456-426614174000
```

**Expected Response (200 OK):**
```json
[
  {
    "role_uuid": "role-123e4567-e89b-12d3-a456-426614174000",
    "role_name": "System Administrator",
    "description": "Full system access role",
    "organization_uuid": "org-123e4567-e89b-12d3-a456-426614174000",
    "role_management_type": "SYSTEM_MANAGED",
    "policy": {
      "permissions": [
        {
          "resource": "*",
          "actions": ["*"]
        }
      ]
    },
    "created_at": "2024-01-15T10:30:00Z",
    "created_by": "system"
  }
]
```

#### 3.5 Update Role
**Purpose**: Modify existing role permissions and details
**Business Value**: Adapt role permissions as organizational needs change
**Authorization**: Requires 'ROLE' 'UPDATE' permission

**Request:**
```http
PUT /role/role-789e0123-e89b-12d3-a456-426614174000
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
x-app-user-uuid: user-123e4567-e89b-12d3-a456-426614174000
Content-Type: application/json

{
  "role_name": "Senior Project Manager",
  "description": "Enhanced role for senior project managers with additional permissions",
  "organization_uuid": "org-123e4567-e89b-12d3-a456-426614174000",
  "role_management_type": "ORGANIZATION_MANAGED",
  "policy": {
    "permissions": [
      {
        "resource": "PROJECT",
        "actions": ["CREATE", "READ", "UPDATE", "DELETE"]
      },
      {
        "resource": "USER",
        "actions": ["READ", "UPDATE"]
      },
      {
        "resource": "TASK",
        "actions": ["CREATE", "READ", "UPDATE", "DELETE"]
      },
      {
        "resource": "BUDGET",
        "actions": ["READ", "UPDATE"]
      }
    ]
  }
}
```

**Expected Response (200 OK):**
```json
{
  "role_uuid": "role-789e0123-e89b-12d3-a456-426614174000",
  "role_name": "Senior Project Manager",
  "description": "Enhanced role for senior project managers with additional permissions",
  "organization_uuid": "org-123e4567-e89b-12d3-a456-426614174000",
  "role_management_type": "ORGANIZATION_MANAGED",
  "policy": {
    "permissions": [
      {
        "resource": "PROJECT",
        "actions": ["CREATE", "READ", "UPDATE", "DELETE"]
      },
      {
        "resource": "USER",
        "actions": ["READ", "UPDATE"]
      },
      {
        "resource": "TASK",
        "actions": ["CREATE", "READ", "UPDATE", "DELETE"]
      },
      {
        "resource": "BUDGET",
        "actions": ["READ", "UPDATE"]
      }
    ]
  },
  "created_at": "2024-01-15T11:00:00Z",
  "updated_at": "2024-01-15T12:00:00Z",
  "created_by": "user-123e4567-e89b-12d3-a456-426614174000"
}
```

#### 3.6 Delete Role
**Purpose**: Remove a role from the system
**Business Value**: Clean up unused roles and maintain role hygiene
**Authorization**: Requires 'ROLE' 'DELETE' permission

**Request:**
```http
DELETE /role/role-789e0123-e89b-12d3-a456-426614174000
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
x-app-user-uuid: user-123e4567-e89b-12d3-a456-426614174000
```

**Expected Response (200 OK):**
```json
"Role role-789e0123-e89b-12d3-a456-426614174000 deleted successfully"
```

---

### Phase 4: User-Role Assignment Operations

#### 4.1 Assign Role to User
**Purpose**: Grant a specific role to a user
**Business Value**: Implement role-based access control for users
**Authorization**: Requires 'USER_ROLE' 'ASSIGN' permission (currently disabled for bootstrap)

**Request:**
```http
POST /role/user/user-456e7890-e89b-12d3-a456-426614174000/assign
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
x-app-user-uuid: user-123e4567-e89b-12d3-a456-426614174000
Content-Type: application/json

{
  "role_uuid": "role-789e0123-e89b-12d3-a456-426614174000",
  "organization_uuid": "org-123e4567-e89b-12d3-a456-426614174000"
}
```

**Expected Response (200 OK):**
```json
"Role role-789e0123-e89b-12d3-a456-426614174000 assigned successfully to user: user-456e7890-e89b-12d3-a456-426614174000"
```

#### 4.2 Get User Roles
**Purpose**: Retrieve all roles assigned to a specific user
**Business Value**: Audit user permissions and access levels
**Authorization**: No specific permission required

**Request:**
```http
GET /role/user/user-456e7890-e89b-12d3-a456-426614174000
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
```

**Expected Response (200 OK):**
```json
[
  {
    "role_uuid": "role-789e0123-e89b-12d3-a456-426614174000",
    "role_name": "Senior Project Manager",
    "description": "Enhanced role for senior project managers with additional permissions",
    "organization_uuid": "org-123e4567-e89b-12d3-a456-426614174000",
    "role_management_type": "ORGANIZATION_MANAGED",
    "created_at": "2024-01-15T11:00:00Z",
    "created_by": "user-123e4567-e89b-12d3-a456-426614174000"
  }
]
```

#### 4.3 Remove Role from User
**Purpose**: Revoke a specific role from a user
**Business Value**: Remove access when users change roles or leave projects
**Authorization**: No specific permission required

**Request:**
```http
DELETE /role/user/user-456e7890-e89b-12d3-a456-426614174000/roles/role-789e0123-e89b-12d3-a456-426614174000
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
x-app-user-uuid: user-123e4567-e89b-12d3-a456-426614174000
```

**Expected Response (200 OK):**
```json
"Role role-789e0123-e89b-12d3-a456-426614174000 removed successfully from user user-456e7890-e89b-12d3-a456-426614174000"
```

#### 4.4 Assign Admin Role to Existing User
**Purpose**: Grant administrative privileges to an existing user
**Business Value**: Promote users to administrative roles for system management
**Authorization**: Requires 'USER_ROLE' 'ASSIGN_ADMIN' permission (currently disabled for bootstrap)

**Request:**
```http
POST /role/user/user-456e7890-e89b-12d3-a456-426614174000/assign-admin
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
x-app-user-uuid: user-123e4567-e89b-12d3-a456-426614174000
```

**Expected Response (200 OK):**
```json
"Admin role assigned successfully to user: user-456e7890-e89b-12d3-a456-426614174000"
```

#### 4.5 Assign Role by UUID to Existing User
**Purpose**: Assign a specific role to an existing user by role UUID
**Business Value**: Flexible role assignment for different organizational needs
**Authorization**: Requires 'USER_ROLE' 'ASSIGN' permission (currently disabled for bootstrap)

**Request:**
```http
POST /role/user/user-456e7890-e89b-12d3-a456-426614174000/assign-role/role-789e0123-e89b-12d3-a456-426614174000
x-app-org-uuid: org-123e4567-e89b-12d3-a456-426614174000
x-app-user-uuid: user-123e4567-e89b-12d3-a456-426614174000
```

**Expected Response (200 OK):**
```json
"Role role-789e0123-e89b-12d3-a456-426614174000 assigned successfully to user: user-456e7890-e89b-12d3-a456-426614174000"
```

---

### Phase 5: Permission and Security Operations

#### 5.1 Check Permission
**Purpose**: Verify if a user has permission to perform a specific action on a resource
**Business Value**: Implement fine-grained access control and authorization checks
**Authorization**: No specific permission required (removed to avoid circular dependency)

**Request:**
```http
POST /role/permissions/check
Content-Type: application/json

{
  "user_uuid": "user-456e7890-e89b-12d3-a456-426614174000",
  "organization_uuid": "org-123e4567-e89b-12d3-a456-426614174000",
  "resource": "PROJECT",
  "action": "CREATE"
}
```

**Expected Response (200 OK):**
```json
{
  "has_permission": true,
  "message": "User has permission to CREATE PROJECT",
  "resource": "PROJECT",
  "action": "CREATE"
}
```

---


### Scenario 1: Complete User Onboarding Flow
1. **Create User** → **Assign Role** → **Verify Permissions**
   - Demonstrates end-to-end user lifecycle management
   - Shows role-based access control in action

### Scenario 2: Role Management and Updates
1. **Create Role** → **Update Role** → **Assign to User** → **Check Permissions**
   - Shows how roles can be modified and their impact on users
   - Demonstrates permission inheritance

### Scenario 3: Organizational Hierarchy
1. **Create Multiple Users** → **Set Reporting Relationships** → **View Hierarchy**
   - Shows organizational structure management
   - Demonstrates reporting relationship tracking

### Scenario 4: Security and Access Control
1. **Create Different Role Types** → **Assign to Users** → **Test Permission Checks**
   - Shows system vs organization-managed roles
   - Demonstrates permission validation

---

## Common Error Responses

### 400 Bad Request
```json
{
  "message": "Validation failed",
  "errors": [
    {
      "field": "username",
      "message": "Username is required"
    }
  ]
}
```

### 401 Unauthorized
```json
{
  "message": "Access denied",
  "error": "Insufficient permissions"
}
```

### 404 Not Found
```json
{
  "message": "Resource not found",
  "error": "User with ID user-456e7890-e89b-12d3-a456-426614174000 not found"
}
```

### 500 Internal Server Error
```json
{
  "message": "Internal server error",
  "error": "An unexpected error occurred"
}
```

---

## Postman Collection Setup

### Environment Variables
Create a Postman environment with these variables:
```
base_url: http://localhost:8080
org_uuid: org-123e4567-e89b-12d3-a456-426614174000
user_uuid: user-123e4567-e89b-12d3-a456-426614174000
session_uuid: session-123e4567-e89b-12d3-a456-426614174000
trace_id: trace-123e4567-e89b-12d3-a456-426614174000
region_id: us-east-1
```

### Collection Structure
1. **Bootstrap Setup**
2. **User Management**
3. **Role Management**
4. **User-Role Assignments**
5. **Permission Checks**

---

## Testing Best Practices

1. **Start with Bootstrap**: Always begin with bootstrap endpoints to set up the system
2. **Use Realistic Data**: Use proper UUIDs and realistic user information
3. **Test Error Cases**: Include invalid data to test validation
4. **Verify Responses**: Check that all expected fields are present
5. **Clean Up**: Consider deactivating test users after testing
6. **Document Issues**: Note any unexpected behaviors or errors

This comprehensive guide provides everything needed to test the User Management Service thoroughly and demonstrate its capabilities to management.
