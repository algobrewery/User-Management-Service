# User Management Service

A comprehensive REST API service for managing users, built with Spring Boot and deployed on AWS ECS with API Gateway integration and PostgreSQL RDS storage.

## 🏗️ Architecture Overview

```
Internet → API Gateway → ECS Fargate → RDS PostgreSQL
                    ↓
              CloudWatch Logs
```

### Deployment Stack
- **Container Platform**: AWS ECS Fargate
- **API Gateway**: AWS API Gateway for external access
- **Database**: Amazon RDS PostgreSQL
- **Monitoring**: AWS CloudWatch
- **Authentication**: API Key-based authentication via Client Management Service

## 🔐 Authentication

All endpoints (except health checks) require API key authentication:
- **Header**: `x-api-key`
- **Validation**: Keys are validated against the Client Management Service
- **Service URL**: `https://qokdavzgh7.execute-api.us-east-1.amazonaws.com/prod/`

## 📋 Required Headers

All API endpoints require the following headers:

| Header | Description | Example |
|--------|-------------|---------|
| `x-api-key` | API key for authentication | `your-api-key-here` |
| `x-app-org-uuid` | Organization UUID | `1d2e3f4a-567b-4c8d-910e-abc123456789` |
| `x-app-user-uuid` | User UUID making the request | `790b5bc8-820d-4a68-a12d-550cfaca14d5` |
| `x-app-client-user-session-uuid` | Session UUID | `session-12345` |
| `x-app-trace-id` | Trace ID for request tracking | `trace-67890` |
| `x-app-region-id` | Region identifier | `us-east-1` |

## 🚀 API Endpoints

### 1. Create User
Creates a new user in the system.

**Endpoint**: `POST /user`

**Request Body**:
```json
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
    "email": "john.doe@example.com",
    "verificationStatus": "VERIFIED"
  },
  "employmentInfoList": [
    {
      "startDate": "2024-01-01T00:00:00",
      "endDate": null,
      "jobTitle": "Software Engineer",
      "organizationUnit": "Engineering",
      "reportingManager": "manager-uuid",
      "extensionsData": {
        "department": "Backend",
        "level": "Senior"
      }
    }
  ]
}
```

**Response**:
```json
{
  "userId": "user-uuid-123",
  "username": "john.doe",
  "status": "ACTIVE",
  "message": "User created successfully",
  "httpStatus": "CREATED"
}
```

**Postman Collection**:
```bash
curl -X POST "https://your-api-gateway-url/user" \
  -H "Content-Type: application/json" \
  -H "x-api-key: your-api-key" \
  -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
  -H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
  -H "x-app-client-user-session-uuid: session-12345" \
  -H "x-app-trace-id: trace-67890" \
  -H "x-app-region-id: us-east-1" \
  -d '{
    "username": "john.doe",
    "firstName": "John",
    "lastName": "Doe",
    "phoneInfo": {
      "number": "1234567890",
      "countryCode": 1,
      "verificationStatus": "VERIFIED"
    },
    "emailInfo": {
      "email": "john.doe@example.com",
      "verificationStatus": "VERIFIED"
    },
    "employmentInfoList": [
      {
        "startDate": "2024-01-01T00:00:00",
        "jobTitle": "Software Engineer",
        "organizationUnit": "Engineering",
        "extensionsData": {}
      }
    ]
  }'
```

### 2. Get User by ID
Retrieves a specific user by their ID.

**Endpoint**: `GET /user/{userId}`

**Path Parameters**:
- `userId` (string): The unique identifier of the user

**Response**:
```json
{
  "userId": "user-uuid-123",
  "username": "john.doe",
  "firstName": "John",
  "middleName": "Michael",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "1234567890",
  "startDate": "2024-01-01T00:00:00",
  "endDate": null,
  "status": "ACTIVE",
  "jobProfiles": [
    {
      "jobProfileUuid": "job-profile-uuid",
      "jobTitle": "Software Engineer",
      "startDate": "2024-01-01T00:00:00",
      "endDate": null,
      "reportingManager": "manager-uuid",
      "reportees": ["reportee-uuid-1", "reportee-uuid-2"],
      "organizationUnit": "Engineering",
      "extensionsData": {
        "department": "Backend",
        "level": "Senior"
      }
    }
  ],
  "httpStatus": "OK"
}
```

**Postman Collection**:
```bash
curl -X GET "https://your-api-gateway-url/user/user-uuid-123" \
  -H "x-api-key: your-api-key" \
  -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
  -H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
  -H "x-app-client-user-session-uuid: session-12345" \
  -H "x-app-trace-id: trace-67890" \
  -H "x-app-region-id: us-east-1"
```

### 3. Update User
Updates an existing user's information.

**Endpoint**: `PUT /user/{userId}`

**Path Parameters**:
- `userId` (string): The unique identifier of the user

**Request Body**:
```json
{
  "username": "john.doe.updated",
  "firstName": "John",
  "middleName": "Michael",
  "lastName": "Doe",
  "status": "ACTIVE",
  "phoneInfo": {
    "number": "9876543210",
    "countryCode": 1,
    "verificationStatus": "VERIFIED"
  },
  "emailInfo": {
    "email": "john.doe.updated@example.com",
    "verificationStatus": "VERIFIED"
  },
  "employmentInfo": {
    "startDate": "2024-01-01T00:00:00",
    "jobTitle": "Senior Software Engineer",
    "organizationUnit": "Engineering",
    "reportingManager": "new-manager-uuid",
    "extensionsData": {
      "department": "Backend",
      "level": "Senior"
    }
  }
}
```

**Response**:
```json
{
  "userId": "user-uuid-123",
  "username": "john.doe.updated",
  "status": "ACTIVE",
  "message": "User updated successfully",
  "httpStatus": "OK"
}
```

**Postman Collection**:
```bash
curl -X PUT "https://your-api-gateway-url/user/user-uuid-123" \
  -H "Content-Type: application/json" \
  -H "x-api-key: your-api-key" \
  -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
  -H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
  -H "x-app-client-user-session-uuid: session-12345" \
  -H "x-app-trace-id: trace-67890" \
  -H "x-app-region-id: us-east-1" \
  -d '{
    "firstName": "John",
    "lastName": "Doe Updated",
    "phoneInfo": {
      "number": "9876543210",
      "countryCode": 1,
      "verificationStatus": "VERIFIED"
    }
  }'
```

### 4. Deactivate User
Deactivates a user (soft delete).

**Endpoint**: `DELETE /user/{userId}`

**Path Parameters**:
- `userId` (string): The unique identifier of the user

**Response**:
```json
{
  "userId": "user-uuid-123",
  "status": "INACTIVE",
  "message": "User deactivated successfully",
  "httpStatus": "OK"
}
```

**Postman Collection**:
```bash
curl -X DELETE "https://your-api-gateway-url/user/user-uuid-123" \
  -H "x-api-key: your-api-key" \
  -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789"
```

### 5. List Users with Filtering
Retrieves a paginated list of users with optional filtering.

**Endpoint**: `POST /users/filter`

**Request Body**:
```json
{
  "filterCriteria": {
    "attributes": [
      {
        "name": "status",
        "operator": "EQUALS",
        "value": "ACTIVE"
      },
      {
        "name": "jobTitle",
        "operator": "CONTAINS",
        "value": "Engineer"
      }
    ]
  },
  "selector": {
    "base_attributes": ["userId", "username", "firstName", "lastName", "email"],
    "extensions": ["department", "level"]
  },
  "page": 0,
  "size": 10,
  "sortBy": "username",
  "sortDirection": "asc"
}
```

**Response**:
```json
{
  "users": [
    {
      "userId": "user-uuid-123",
      "username": "john.doe",
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@example.com",
      "department": "Backend",
      "level": "Senior"
    }
  ],
  "totalElements": 25,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 10,
  "hasNext": true,
  "hasPrevious": false
}
```

**Postman Collection**:
```bash
curl -X POST "https://your-api-gateway-url/users/filter" \
  -H "Content-Type: application/json" \
  -H "x-api-key: your-api-key" \
  -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
  -H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
  -H "x-app-client-user-session-uuid: session-12345" \
  -H "x-app-trace-id: trace-67890" \
  -H "x-app-region-id: us-east-1" \
  -d '{
    "page": 0,
    "size": 10,
    "sortBy": "username",
    "sortDirection": "asc"
  }'
```

### 6. Get User Hierarchy
Retrieves the reporting hierarchy for a specific user.

**Endpoint**: `GET /users/{userId}/hierarchy`

**Path Parameters**:
- `userId` (string): The unique identifier of the user

**Response**:
```json
{
  "userId": "user-uuid-123",
  "reportingManager": {
    "userId": "manager-uuid",
    "name": "Jane Smith"
  },
  "reportees": [
    {
      "userId": "reportee-uuid-1",
      "name": "Alice Johnson"
    },
    {
      "userId": "reportee-uuid-2",
      "name": "Bob Wilson"
    }
  ]
}
```

**Postman Collection**:
```bash
curl -X GET "https://your-api-gateway-url/users/user-uuid-123/hierarchy" \
  -H "x-api-key: your-api-key" \
  -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789"
```

## 🏥 Health Check Endpoints

### Health Check
**Endpoint**: `GET /actuator/health`
- No authentication required
- Returns service health status

### Application Info
**Endpoint**: `GET /actuator/info`
- No authentication required
- Returns application information

## 🗄️ Database Schema

The service uses PostgreSQL RDS with the following key tables:

### Users Table
```sql
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255),
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    org_uuid UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Employment Info Table
```sql
CREATE TABLE employment_info (
    employment_id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(user_id),
    job_title VARCHAR(255) NOT NULL,
    organization_unit VARCHAR(255) NOT NULL,
    reporting_manager UUID,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    extensions_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 🚀 Deployment

### AWS ECS Deployment
The service is containerized and deployed on AWS ECS Fargate with:
- **Auto Scaling**: Based on CPU and memory utilization
- **Load Balancing**: Application Load Balancer
- **Service Discovery**: AWS Cloud Map
- **Logging**: CloudWatch Logs

### API Gateway Integration
- **Custom Domain**: Configured with SSL certificate
- **Rate Limiting**: Configured per API key
- **Request/Response Transformation**: JSON to JSON mapping
- **CORS**: Enabled for web applications

### Environment Variables
```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://your-rds-endpoint:5432/userdb
SPRING_DATASOURCE_USERNAME=your-db-username
SPRING_DATASOURCE_PASSWORD=your-db-password

# Client Management Service
CLIENT_MANAGEMENT_SERVICE_URL=https://qokdavzgh7.execute-api.us-east-1.amazonaws.com/prod/
CLIENT_MANAGEMENT_SERVICE_TIMEOUT=5

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod
```

## 🔧 Local Development

### Prerequisites
- Java 17+
- PostgreSQL 12+
- Gradle 7+

### Setup
1. Clone the repository
2. Configure database connection in `application-local.properties`
3. Run the application:
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run integration tests
./gradlew integrationTest
```

## 📊 Monitoring and Logging

### CloudWatch Metrics
- **Custom Metrics**: User creation rate, API response times
- **System Metrics**: CPU, Memory, Network utilization
- **Database Metrics**: Connection pool, query performance

### Logging
- **Structured Logging**: JSON format for better parsing
- **Log Levels**: Configurable per environment
- **Correlation IDs**: Request tracing across services

## 🔒 Security

### API Key Authentication
- Keys validated against Client Management Service
- Rate limiting per API key
- Request/response logging for audit

### Data Protection
- Sensitive data encryption at rest
- TLS 1.2+ for data in transit
- Database connection encryption

## 📈 Performance

### Caching Strategy
- Redis for frequently accessed user data
- Database query optimization
- Connection pooling

### Scalability
- Horizontal scaling with ECS
- Database read replicas
- CDN for static content

## 🐛 Error Handling

### Standard Error Response
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Invalid request parameters",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/user",
  "details": [
    {
      "field": "email",
      "message": "Invalid email format"
    }
  ]
}
```

### HTTP Status Codes
- `200` - Success
- `201` - Created
- `400` - Bad Request
- `401` - Unauthorized (Invalid API Key)
- `404` - Not Found
- `409` - Conflict (Duplicate Resource)
- `500` - Internal Server Error

## 📞 Support

For technical support or questions:
- **Repository**: [algobrewery/User-Management-Service](https://github.com/algobrewery/User-Management-Service)
- **Issues**: Create an issue in the GitHub repository
- **Documentation**: Check the `/docs` folder for additional documentation

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.#   T e s t   O I D C   f i x  
 