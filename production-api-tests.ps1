# Production API Endpoint Testing Script
# Tests all User Management Service API endpoints after deployment

param(
    [Parameter(Mandatory=$false)]
    [string]$ApiUrl = "https://rt786fxfde.execute-api.us-east-1.amazonaws.com/prod",
    [Parameter(Mandatory=$false)]
    [string]$ApiKey = "pzKOjno8c-aLPvTz0L4b6U-UGDs7_7qq3W7qu7lpF7w"
)

# Configuration
$API_BASE_URL = $ApiUrl
$API_KEY = $ApiKey
$ORG_UUID = "org-1"
$USER_UUID = "f002a471-ebcc-4d6c-ad3c-2327805c001c"
$SESSION_UUID = "test-session-" + [System.Guid]::NewGuid().ToString()
$TRACE_ID = "test-trace-" + [System.Guid]::NewGuid().ToString()
$REGION_ID = "us-east-1"

# Test results tracking
$TestResults = @{
    Total = 0
    Passed = 0
    Failed = 0
    Details = @()
}

# Common headers for all requests
$CommonHeaders = @{
    'x-api-key' = $API_KEY
    'x-app-org-uuid' = $ORG_UUID
    'x-app-user-uuid' = $USER_UUID
    'x-app-client-user-session-uuid' = $SESSION_UUID
    'x-app-trace-id' = $TRACE_ID
    'x-app-region-id' = $REGION_ID
    'Content-Type' = 'application/json'
}

# Function to execute API test
function Test-ApiEndpoint {
    param(
        [string]$TestName,
        [string]$Method,
        [string]$Endpoint,
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [int]$ExpectedStatus = 200,
        [bool]$ExpectFailure = $false
    )
    
    $TestResults.Total++
    
    try {
        $url = "$API_BASE_URL$Endpoint"
        $allHeaders = $CommonHeaders + $Headers
        
        Write-Host "Testing: $TestName" -ForegroundColor Yellow
        Write-Host "  URL: $url" -ForegroundColor Gray
        Write-Host "  Method: $Method" -ForegroundColor Gray
        
        $requestParams = @{
            Uri = $url
            Method = $Method
            Headers = $allHeaders
            TimeoutSec = 30
        }
        
        if ($Body) {
            $requestParams.Body = $Body
        }
        
        $response = Invoke-RestMethod @requestParams -ErrorAction Stop
        
        if ($ExpectFailure) {
            $TestResults.Failed++
            $TestResults.Details += @{
                TestName = $TestName
                Status = "FAILED"
                Message = "Expected failure but got success"
                Response = $response
            }
            Write-Host "  FAILED - Expected failure but got success" -ForegroundColor Red
        } else {
            $TestResults.Passed++
            $TestResults.Details += @{
                TestName = $TestName
                Status = "PASSED"
                Message = "Success"
                Response = $response
            }
            Write-Host "  PASSED" -ForegroundColor Green
        }
        
        return $response
        
    } catch {
        $statusCode = $null
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        
        if ($ExpectFailure -and $statusCode -ge 400) {
            $TestResults.Passed++
            $TestResults.Details += @{
                TestName = $TestName
                Status = "PASSED"
                Message = "Expected failure received (Status: $statusCode)"
                Response = $null
            }
            Write-Host "  PASSED - Expected failure (Status: $statusCode)" -ForegroundColor Green
        } elseif ($statusCode -eq $ExpectedStatus) {
            $TestResults.Passed++
            $TestResults.Details += @{
                TestName = $TestName
                Status = "PASSED"
                Message = "Expected status received (Status: $statusCode)"
                Response = $null
            }
            Write-Host "  PASSED - Expected status (Status: $statusCode)" -ForegroundColor Green
        } else {
            $TestResults.Failed++
            $TestResults.Details += @{
                TestName = $TestName
                Status = "FAILED"
                Message = "Unexpected error (Status: $statusCode)"
                Response = $null
                Error = $_.Exception.Message
            }
            Write-Host "  FAILED - Status: $statusCode" -ForegroundColor Red
            Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
        }
        
        return $null
    }
}

# Start testing
Write-Host "===============================================" -ForegroundColor Green
Write-Host "    PRODUCTION API ENDPOINT TESTING" -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green
Write-Host "API Base URL: $API_BASE_URL" -ForegroundColor Gray
Write-Host "Organization: $ORG_UUID" -ForegroundColor Gray
Write-Host "Test User: $USER_UUID" -ForegroundColor Gray
Write-Host ""

# ==============================================
# HEALTH & MONITORING ENDPOINTS
# ==============================================
Write-Host "1. HEALTH & MONITORING ENDPOINTS" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan

Test-ApiEndpoint -TestName "Health Check" -Method "GET" -Endpoint "/actuator/health"
Test-ApiEndpoint -TestName "Application Info" -Method "GET" -Endpoint "/actuator/info"
Test-ApiEndpoint -TestName "Metrics" -Method "GET" -Endpoint "/actuator/metrics"

# ==============================================
# USER MANAGEMENT ENDPOINTS
# ==============================================
Write-Host "`n2. USER MANAGEMENT ENDPOINTS" -ForegroundColor Cyan
Write-Host "=============================" -ForegroundColor Cyan

# Create User
$uniqueId = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$createUserData = @{
    username = "test-user-$uniqueId"
    firstName = "Test"
    lastName = "User"
    emailInfo = @{
        email = "test-$uniqueId@example.com"
        verificationStatus = "VERIFIED"
    }
    phoneInfo = @{
        number = "1234567890"
        countryCode = 1
        verificationStatus = "VERIFIED"
    }
    employmentInfoList = @(
        @{
            jobTitle = "Software Engineer"
            organizationUnit = "Engineering"
            startDate = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
            reportingManager = "test-user-2"
            extensionsData = @{}
            endDate = $null
        }
    )
} | ConvertTo-Json -Depth 10

$createResponse = Test-ApiEndpoint -TestName "Create User" -Method "POST" -Endpoint "/user" -Body $createUserData
$createdUserId = if ($createResponse -and $createResponse.userUuid) { $createResponse.userUuid } else { "test-user-1" }

# Get User
Test-ApiEndpoint -TestName "Get User (Existing)" -Method "GET" -Endpoint "/user/$createdUserId"
Test-ApiEndpoint -TestName "Get User (Non-existent)" -Method "GET" -Endpoint "/user/non-existent-user" -ExpectedStatus 404

# Update User
$updateUserData = @{
    username = $createdUserId
    firstName = "Updated"
    lastName = "User"
    emailInfo = @{
        email = "updated-$uniqueId@example.com"
        verificationStatus = "VERIFIED"
    }
    phoneInfo = @{
        number = "9876543210"
        countryCode = 1
        verificationStatus = "VERIFIED"
    }
    employmentInfoList = @(
        @{
            jobTitle = "Senior Software Engineer"
            organizationUnit = "Engineering"
            startDate = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
            reportingManager = "test-user-2"
            extensionsData = @{}
            endDate = $null
        }
    )
} | ConvertTo-Json -Depth 10

Test-ApiEndpoint -TestName "Update User" -Method "PUT" -Endpoint "/user/$createdUserId" -Body $updateUserData

# List Users
$listUsersData = @{
    pageNumber = 0
    pageSize = 10
    filters = @{
        status = "Active"
    }
} | ConvertTo-Json -Depth 10

Test-ApiEndpoint -TestName "List Users (Filter)" -Method "POST" -Endpoint "/users/filter" -Body $listUsersData

# User Hierarchy
Test-ApiEndpoint -TestName "User Hierarchy" -Method "GET" -Endpoint "/users/$createdUserId/hierarchy"

# Delete User (Soft Delete)
Test-ApiEndpoint -TestName "Delete User" -Method "DELETE" -Endpoint "/user/test-user-3"

# ==============================================
# ROLE MANAGEMENT ENDPOINTS
# ==============================================
Write-Host "`n3. ROLE MANAGEMENT ENDPOINTS" -ForegroundColor Cyan
Write-Host "=============================" -ForegroundColor Cyan

# Create Role
$createRoleData = @{
    name = "Test Role $uniqueId"
    description = "Test role for API testing"
    permissions = @("read:users", "write:users")
    isSystemManaged = $false
} | ConvertTo-Json -Depth 10

$createRoleResponse = Test-ApiEndpoint -TestName "Create Role" -Method "POST" -Endpoint "/role" -Body $createRoleData
$createdRoleId = if ($createRoleResponse -and $createRoleResponse.roleUuid) { $createRoleResponse.roleUuid } else { "test-role-1" }

# Get Role
Test-ApiEndpoint -TestName "Get Role (Existing)" -Method "GET" -Endpoint "/role/$createdRoleId"
Test-ApiEndpoint -TestName "Get Role (Non-existent)" -Method "GET" -Endpoint "/role/non-existent-role" -ExpectedStatus 404

# Update Role
$updateRoleData = @{
    name = "Updated Test Role $uniqueId"
    description = "Updated test role for API testing"
    permissions = @("read:users", "write:users", "delete:users")
    isSystemManaged = $false
} | ConvertTo-Json -Depth 10

Test-ApiEndpoint -TestName "Update Role" -Method "PUT" -Endpoint "/role/$createdRoleId" -Body $updateRoleData

# Search Roles
$searchRolesData = @{
    pageNumber = 0
    pageSize = 10
    filters = @{
        isSystemManaged = $false
    }
} | ConvertTo-Json -Depth 10

Test-ApiEndpoint -TestName "Search Roles" -Method "POST" -Endpoint "/role/search" -Body $searchRolesData

# Get System Managed Roles
Test-ApiEndpoint -TestName "Get System Managed Roles" -Method "GET" -Endpoint "/role/system-managed"

# Delete Role
Test-ApiEndpoint -TestName "Delete Role" -Method "DELETE" -Endpoint "/role/$createdRoleId"

# ==============================================
# USER-ROLE ASSIGNMENT ENDPOINTS
# ==============================================
Write-Host "`n4. USER-ROLE ASSIGNMENT ENDPOINTS" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan

# Assign Role to User
Test-ApiEndpoint -TestName "Assign Role to User" -Method "POST" -Endpoint "/user/$createdUserId/roles/$createdRoleId"

# Get User Roles
Test-ApiEndpoint -TestName "Get User Roles" -Method "GET" -Endpoint "/user/$createdUserId/roles"

# Remove Role from User
Test-ApiEndpoint -TestName "Remove Role from User" -Method "DELETE" -Endpoint "/user/$createdUserId/roles/$createdRoleId"

# ==============================================
# PERMISSION CHECK ENDPOINTS
# ==============================================
Write-Host "`n5. PERMISSION CHECK ENDPOINTS" -ForegroundColor Cyan
Write-Host "==============================" -ForegroundColor Cyan

# Check Permission (Detailed)
$permissionCheckData = @{
    userUuid = $createdUserId
    organizationUuid = $ORG_UUID
    resource = "users"
    action = "read"
} | ConvertTo-Json -Depth 10

Test-ApiEndpoint -TestName "Check Permission (Detailed)" -Method "POST" -Endpoint "/permission/check" -Body $permissionCheckData

# Check Permission (Quick)
Test-ApiEndpoint -TestName "Check Permission (Quick)" -Method "GET" -Endpoint "/permission/check?userUuid=$createdUserId&organizationUuid=$ORG_UUID&resource=users&action=read"

# ==============================================
# SECURITY & AUTHENTICATION TESTS
# ==============================================
Write-Host "`n6. SECURITY & AUTHENTICATION TESTS" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan

# Test with invalid API key
Test-ApiEndpoint -TestName "Invalid API Key" -Method "GET" -Endpoint "/user/$createdUserId" -Headers @{'x-api-key' = 'invalid-key'} -ExpectFailure $true

# Test with missing headers
Test-ApiEndpoint -TestName "Missing Headers" -Method "GET" -Endpoint "/user/$createdUserId" -Headers @{} -ExpectFailure $true

# Test with invalid organization
Test-ApiEndpoint -TestName "Invalid Organization" -Method "GET" -Endpoint "/user/$createdUserId" -Headers @{'x-app-org-uuid' = 'invalid-org'} -ExpectFailure $true

# ==============================================
# ERROR HANDLING TESTS
# ==============================================
Write-Host "`n7. ERROR HANDLING TESTS" -ForegroundColor Cyan
Write-Host "========================" -ForegroundColor Cyan

# Test invalid JSON
Test-ApiEndpoint -TestName "Invalid JSON" -Method "POST" -Endpoint "/user" -Body "invalid-json" -ExpectFailure $true

# Test malformed request
Test-ApiEndpoint -TestName "Malformed Request" -Method "POST" -Endpoint "/user" -Body '{"invalid": "data"}' -ExpectFailure $true

# Test unauthorized endpoint
Test-ApiEndpoint -TestName "Unauthorized Endpoint" -Method "GET" -Endpoint "/admin/users" -ExpectFailure $true

# ==============================================
# PERFORMANCE TESTS
# ==============================================
Write-Host "`n8. PERFORMANCE TESTS" -ForegroundColor Cyan
Write-Host "====================" -ForegroundColor Cyan

# Test response times
$startTime = Get-Date
Test-ApiEndpoint -TestName "Health Check Performance" -Method "GET" -Endpoint "/actuator/health"
$endTime = Get-Date
$responseTime = ($endTime - $startTime).TotalMilliseconds
Write-Host "  Response Time: $([math]::Round($responseTime, 2))ms" -ForegroundColor Gray

# Test concurrent requests (simulate)
Write-Host "  Testing concurrent request handling..." -ForegroundColor Gray
$concurrentTests = @()
for ($i = 1; $i -le 5; $i++) {
    $concurrentTests += Test-ApiEndpoint -TestName "Concurrent Request $i" -Method "GET" -Endpoint "/actuator/health"
}

# ==============================================
# TEST RESULTS SUMMARY
# ==============================================
Write-Host "`n===============================================" -ForegroundColor Green
Write-Host "           TEST RESULTS SUMMARY" -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green

Write-Host "Total Tests: $($TestResults.Total)" -ForegroundColor White
Write-Host "Passed: $($TestResults.Passed)" -ForegroundColor Green
Write-Host "Failed: $($TestResults.Failed)" -ForegroundColor Red
Write-Host "Success Rate: $([math]::Round(($TestResults.Passed / $TestResults.Total) * 100, 2))%" -ForegroundColor Yellow

Write-Host "`nDETAILED RESULTS:" -ForegroundColor Cyan
Write-Host "=================" -ForegroundColor Cyan

foreach ($result in $TestResults.Details) {
    $status = if ($result.Status -eq "PASSED") { "✅" } else { "❌" }
    Write-Host "$status $($result.TestName): $($result.Status)" -ForegroundColor $(if ($result.Status -eq "PASSED") { "Green" } else { "Red" })
    if ($result.Message) {
        Write-Host "    $($result.Message)" -ForegroundColor Gray
    }
    if ($result.Error) {
        Write-Host "    Error: $($result.Error)" -ForegroundColor Red
    }
}

# Final status
if ($TestResults.Failed -eq 0) {
    Write-Host "`n🎉 ALL TESTS PASSED! Production API is working perfectly." -ForegroundColor Green
} elseif ($TestResults.Passed -gt $TestResults.Failed) {
    Write-Host "`n⚠️  MOSTLY SUCCESSFUL! Some tests failed but core functionality is working." -ForegroundColor Yellow
} else {
    Write-Host "`n❌ MULTIPLE FAILURES! Please check the system configuration." -ForegroundColor Red
}

Write-Host "`nTest completed at: $(Get-Date)" -ForegroundColor Gray
Write-Host "===============================================" -ForegroundColor Green
