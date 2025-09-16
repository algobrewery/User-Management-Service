# Quick API Test Script - Single Script for All Endpoints
# Tests core functionality of User Management Service

param(
    [string]$ApiUrl = "https://rt786fxfde.execute-api.us-east-1.amazonaws.com/prod",
    [string]$ApiKey = "pzKOjno8c-aLPvTz0L4b6U-UGDs7_7qq3W7qu7lpF7w"
)

$API_BASE_URL = $ApiUrl
$API_KEY = $ApiKey
$ORG_UUID = "org-1"
$USER_UUID = "f002a471-ebcc-4d6c-ad3c-2327805c001c"

Write-Host "===============================================" -ForegroundColor Green
Write-Host "        QUICK API ENDPOINT TEST" -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green
Write-Host "API URL: $API_BASE_URL" -ForegroundColor Gray
Write-Host ""

$headers = @{
    'x-api-key' = $API_KEY
    'x-app-org-uuid' = $ORG_UUID
    'x-app-user-uuid' = $USER_UUID
    'x-app-client-user-session-uuid' = "test-session"
    'x-app-trace-id' = "test-trace"
    'x-app-region-id' = "us-east-1"
    'Content-Type' = 'application/json'
}

$totalTests = 0
$passedTests = 0

# Test function
function Test-Endpoint {
    param([string]$Name, [string]$Method, [string]$Endpoint, [string]$Body = $null)
    
    $totalTests++
    try {
        $url = "$API_BASE_URL$Endpoint"
        Write-Host "Testing: $Name" -ForegroundColor Yellow
        
        $params = @{
            Uri = $url
            Method = $Method
            Headers = $headers
            TimeoutSec = 30
        }
        
        if ($Body) { $params.Body = $Body }
        
        $response = Invoke-RestMethod @params -ErrorAction Stop
        Write-Host "  ✅ PASSED" -ForegroundColor Green
        $passedTests++
        return $response
    } catch {
        $statusCode = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { "Unknown" }
        Write-Host "  ❌ FAILED (Status: $statusCode)" -ForegroundColor Red
        return $null
    }
}

# 1. HEALTH ENDPOINTS
Write-Host "1. HEALTH & MONITORING" -ForegroundColor Cyan
Test-Endpoint "Health Check" "GET" "/actuator/health"
Test-Endpoint "App Info" "GET" "/actuator/info"

# 2. USER MANAGEMENT
Write-Host "`n2. USER MANAGEMENT" -ForegroundColor Cyan

# Create user
$userData = @{
    username = "test-$(Get-Random)"
    firstName = "Test"
    lastName = "User"
    emailInfo = @{
        email = "test$(Get-Random)@example.com"
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
        }
    )
} | ConvertTo-Json -Depth 10

$createResponse = Test-Endpoint "Create User" "POST" "/user" $userData
$userId = if ($createResponse -and $createResponse.userUuid) { $createResponse.userUuid } else { "test-user-1" }

# Other user operations
Test-Endpoint "Get User" "GET" "/user/$userId"
Test-Endpoint "List Users" "POST" "/users/filter" '{"pageNumber":0,"pageSize":5,"filters":{"status":"Active"}}'
Test-Endpoint "User Hierarchy" "GET" "/users/$userId/hierarchy"

# 3. ROLE MANAGEMENT
Write-Host "`n3. ROLE MANAGEMENT" -ForegroundColor Cyan

$roleData = @{
    name = "Test Role $(Get-Random)"
    description = "Test role"
    permissions = @("read:users")
    isSystemManaged = $false
} | ConvertTo-Json -Depth 10

$roleResponse = Test-Endpoint "Create Role" "POST" "/role" $roleData
$roleId = if ($roleResponse -and $roleResponse.roleUuid) { $roleResponse.roleUuid } else { "test-role-1" }

Test-Endpoint "Get Role" "GET" "/role/$roleId"
Test-Endpoint "Search Roles" "POST" "/role/search" '{"pageNumber":0,"pageSize":5}'
Test-Endpoint "System Roles" "GET" "/role/system-managed"

# 4. USER-ROLE ASSIGNMENTS
Write-Host "`n4. USER-ROLE ASSIGNMENTS" -ForegroundColor Cyan
Test-Endpoint "Assign Role" "POST" "/user/$userId/roles/$roleId"
Test-Endpoint "Get User Roles" "GET" "/user/$userId/roles"
Test-Endpoint "Remove Role" "DELETE" "/user/$userId/roles/$roleId"

# 5. PERMISSION CHECKS
Write-Host "`n5. PERMISSION CHECKS" -ForegroundColor Cyan
Test-Endpoint "Check Permission" "POST" "/permission/check" '{"userUuid":"'$userId'","organizationUuid":"'$ORG_UUID'","resource":"users","action":"read"}'

# 6. SECURITY TESTS
Write-Host "`n6. SECURITY TESTS" -ForegroundColor Cyan

# Test with invalid API key
try {
    $totalTests++
    $invalidHeaders = $headers.Clone()
    $invalidHeaders['x-api-key'] = 'invalid-key'
    Invoke-RestMethod -Uri "$API_BASE_URL/user/$userId" -Method GET -Headers $invalidHeaders -TimeoutSec 30
    Write-Host "Testing: Invalid API Key" -ForegroundColor Yellow
    Write-Host "  ❌ FAILED (Should have been rejected)" -ForegroundColor Red
} catch {
    Write-Host "Testing: Invalid API Key" -ForegroundColor Yellow
    Write-Host "  ✅ PASSED (Rejected as expected)" -ForegroundColor Green
    $passedTests++
}

# Test missing headers
try {
    $totalTests++
    Invoke-RestMethod -Uri "$API_BASE_URL/user/$userId" -Method GET -Headers @{} -TimeoutSec 30
    Write-Host "Testing: Missing Headers" -ForegroundColor Yellow
    Write-Host "  ❌ FAILED (Should have been rejected)" -ForegroundColor Red
} catch {
    Write-Host "Testing: Missing Headers" -ForegroundColor Yellow
    Write-Host "  ✅ PASSED (Rejected as expected)" -ForegroundColor Green
    $passedTests++
}

# RESULTS
Write-Host "`n===============================================" -ForegroundColor Green
Write-Host "              TEST RESULTS" -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green
Write-Host "Total Tests: $totalTests" -ForegroundColor White
Write-Host "Passed: $passedTests" -ForegroundColor Green
Write-Host "Failed: $($totalTests - $passedTests)" -ForegroundColor Red
Write-Host "Success Rate: $([math]::Round(($passedTests / $totalTests) * 100, 2))%" -ForegroundColor Yellow

if ($passedTests -eq $totalTests) {
    Write-Host "`n🎉 ALL TESTS PASSED! API is working perfectly." -ForegroundColor Green
} elseif ($passedTests -gt ($totalTests / 2)) {
    Write-Host "`n⚠️  MOSTLY SUCCESSFUL! Core functionality is working." -ForegroundColor Yellow
} else {
    Write-Host "`n❌ MULTIPLE FAILURES! Check system configuration." -ForegroundColor Red
}

Write-Host "`nTest completed at: $(Get-Date)" -ForegroundColor Gray
Write-Host "===============================================" -ForegroundColor Green
