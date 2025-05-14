# User-Management-Service
A Spring Boot-based User Management Service with PostgreSQL integration. Supports user creation, retrieval, and filtering with structured JSON APIs, UUID-based IDs, and nested employment, phone, and email info. Designed for scalability and AWS deployment.

# curl command to create user

curl -X POST http://localhost:8080/user \
-H "Content-Type: application/json" \
-H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
-H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
-H "x-app-client-user-session-uuid: session-12345" \
-H "x-app-trace-id: trace-67890" \
-H "x-app-region-id: us-east-1" \
-d '{
  "username": "user1202",
  "firstName": "f1202",
  "middleName": "m1202",
  "lastName": "l1202",
  "phoneInfo": {
    "number": "8080801202",
    "countryCode": "91",
    "verificationStatus": "verified"
  },
  "emailInfo": {
    "email": "user1202@algobrewery.com",
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
    },
    {
      "startDate": "2022-11-29T00:00:00",
      "endDate": "2023-09-19T00:00:00",
      "jobTitle": "Devops Eng 1",
      "organizationUnit": "Technology",
      "reportingManager": "790b5bc8-820d-4a68-a12d-550cfaca14d5",
      "extensionsData": {
        "employmentType": "CONTRACT",
        "primaryLocation": "Bangalore"
      }
    },
    {
      "startDate": "2023-09-22T00:00:00",
      "jobTitle": "Devops Eng 2",
      "organizationUnit": "Technology",
      "reportingManager": "790b5bc8-820d-4a68-a12d-550cfaca14d5",
      "extensionsData": {
        "employmentType": "FULL_TIME",
        "primaryLocation": "Remote"
      }
    }
  ]
}'

# curl command to get user

curl -X GET http://localhost:8080/user/eae72380-4e09-4729-b626-f5f30bce1e47 \
-H "Content-Type: application/json" \
-H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" \
-H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" \
-H "x-app-client-user-session-uuid: session-12345" \
-H "x-app-trace-id: trace-67890" \
-H "x-app-region-id: us-east-1"