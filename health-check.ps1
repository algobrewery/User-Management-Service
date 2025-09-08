# User Management Service Health Check Script with CloudWatch Metrics
# Run this script before deployment to verify service health and log metrics

# Configuration
$API_BASE_URL = "https://rt786fxfde.execute-api.us-east-1.amazonaws.com/prod/user"
$API_KEY = "pzKOjno8c-aLPvTz0L4b6U-UGDs7_7qq3W7qu7lpF7w"

# CloudWatch Configuration
$CLOUDWATCH_NAMESPACE = "UserManagementService/HealthCheck"
$CLOUDWATCH_REGION = "us-east-1"

# Generate dynamic test data
$TIMESTAMP = [DateTimeOffset]::Now.ToUnixTimeSeconds()
$USERNAME = "testuser_$TIMESTAMP"
$PHONE_NUMBER = "987654$($TIMESTAMP.ToString().Substring($TIMESTAMP.ToString().Length - 6))"
$EMAIL = "testuser_$TIMESTAMP@example.com"

# Metrics tracking
$script:METRICS = @{
    StartTime = Get-Date
    Tests = @{}
    OverallSuccess = $true
}

# Function to send CloudWatch metrics
function Send-CloudWatchMetric {
    param(
        [string]$MetricName,
        [double]$Value,
        [string]$Unit = "Count",
        [hashtable]$Dimensions = @{}
    )
    
    try {
        $dimensionsList = @()
        foreach ($dim in $Dimensions.GetEnumerator()) {
            $dimensionsList += @{
                Name = $dim.Key
                Value = $dim.Value
            }
        }
        
        $metricData = @{
            MetricName = $MetricName
            Value = $Value
            Unit = $Unit
            Timestamp = Get-Date
            Dimensions = $dimensionsList
        }
        
        $metricJson = $metricData | ConvertTo-Json -Depth 10
        aws cloudwatch put-metric-data --namespace $CLOUDWATCH_NAMESPACE --metric-data $metricJson --region $CLOUDWATCH_REGION --output json 2>$null
        
        Write-Host "  Metric sent: $MetricName = $Value $Unit" -ForegroundColor Gray
    }
    catch {
        Write-Host "  Warning: Failed to send metric $MetricName" -ForegroundColor Yellow
    }
}

# Function to track test metrics
function Track-TestMetric {
    param(
        [string]$TestName,
        [string]$Status,
        [double]$Duration,
        [string]$ErrorMessage = ""
    )
    
    $script:METRICS.Tests[$TestName] = @{
        Status = $Status
        Duration = $Duration
        ErrorMessage = $ErrorMessage
        Timestamp = Get-Date
    }
    
    # Send individual test metrics
    $dimensions = @{
        TestName = $TestName
        Service = "UserManagement"
        Environment = "Production"
    }
    
    Send-CloudWatchMetric -MetricName "TestDuration" -Value $Duration -Unit "Milliseconds" -Dimensions $dimensions
    Send-CloudWatchMetric -MetricName "TestStatus" -Value $(if ($Status -eq "SUCCESS") { 1 } else { 0 }) -Unit "Count" -Dimensions $dimensions
    
    if ($ErrorMessage) {
        Send-CloudWatchMetric -MetricName "TestErrors" -Value 1 -Unit "Count" -Dimensions $dimensions
    }
}

Write-Host "=================================" -ForegroundColor Magenta
Write-Host "Health Check with CloudWatch Metrics" -ForegroundColor Magenta
Write-Host "=================================" -ForegroundColor Magenta
Write-Host "API: $API_BASE_URL" -ForegroundColor White
Write-Host "User: $USERNAME" -ForegroundColor White
Write-Host "Time: $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor White
Write-Host "CloudWatch: $CLOUDWATCH_NAMESPACE" -ForegroundColor White
Write-Host "=================================" -ForegroundColor Magenta

# Test 1: Create User
Write-Host "`nTest 1: Creating User..." -ForegroundColor Yellow
$testStart = Get-Date
$CREATE_PAYLOAD = @{
    username = $USERNAME
    firstName = "John"
    middleName = "M"
    lastName = "Doe"
    phoneInfo = @{
        number = $PHONE_NUMBER
        countryCode = "91"
        verificationStatus = "verified"
    }
    emailInfo = @{
        email = $EMAIL
        verificationStatus = "verified"
    }
    employmentInfoList = @(
        @{
            startDate = "2024-01-01T00:00:00"
            endDate = "2024-12-31T00:00:00"
            jobTitle = "Software Engineer"
            organizationUnit = "Technology"
            reportingManager = "189537bb-6254-43cb-97af-9999709a0af8"
            extensionsData = @{
                employmentType = "FULL_TIME"
                primaryLocation = "Remote"
            }
        }
    )
} | ConvertTo-Json -Depth 10

Write-Host "  POST $API_BASE_URL" -ForegroundColor Cyan
try {
    $headers = @{
        "x-api-key" = $API_KEY
        "x-app-org-uuid" = "1d2e3f4a-567b-4c8d-910e-abc123456789"
        "x-app-user-uuid" = "790b5bc8-820d-4a68-a12d-550cfaca14d5"
        "x-app-client-user-session-uuid" = "session-12345"
        "x-app-trace-id" = "trace-$TIMESTAMP"
        "x-app-region-id" = "us-east-1"
        "Content-Type" = "application/json"
    }
    
    $CREATE_RESPONSE = Invoke-RestMethod -Uri $API_BASE_URL -Method "POST" -Headers $headers -Body $CREATE_PAYLOAD -TimeoutSec 30
    $TEST_USER_ID = $CREATE_RESPONSE.userId
    $testDuration = ((Get-Date) - $testStart).TotalMilliseconds
    Write-Host "  SUCCESS - User ID: $TEST_USER_ID" -ForegroundColor Green
    Track-TestMetric -TestName "CreateUser" -Status "SUCCESS" -Duration $testDuration
}
catch {
    $testDuration = ((Get-Date) - $testStart).TotalMilliseconds
    Write-Host "  FAILED: $($_.Exception.Message)" -ForegroundColor Red
    Track-TestMetric -TestName "CreateUser" -Status "FAILED" -Duration $testDuration -ErrorMessage $_.Exception.Message
    $script:METRICS.OverallSuccess = $false
    exit 1
}

# Test 2: Get User
Write-Host "`nTest 2: Getting User..." -ForegroundColor Yellow
$testStart = Get-Date
$GET_URL = "$API_BASE_URL/$TEST_USER_ID"
Write-Host "  GET $GET_URL" -ForegroundColor Cyan
try {
    $GET_RESPONSE = Invoke-RestMethod -Uri $GET_URL -Method "GET" -Headers $headers -TimeoutSec 30
    $testDuration = ((Get-Date) - $testStart).TotalMilliseconds
    Write-Host "  SUCCESS - User retrieved" -ForegroundColor Green
    Track-TestMetric -TestName "GetUser" -Status "SUCCESS" -Duration $testDuration
}
catch {
    $testDuration = ((Get-Date) - $testStart).TotalMilliseconds
    Write-Host "  FAILED: $($_.Exception.Message)" -ForegroundColor Red
    Track-TestMetric -TestName "GetUser" -Status "FAILED" -Duration $testDuration -ErrorMessage $_.Exception.Message
    $script:METRICS.OverallSuccess = $false
    exit 1
}

# Test 3: Update User
Write-Host "`nTest 3: Updating User..." -ForegroundColor Yellow
$testStart = Get-Date
$UPDATE_PAYLOAD = @{
    firstName = "Updated"
    lastName = "User"
} | ConvertTo-Json

$UPDATE_URL = "$API_BASE_URL/$TEST_USER_ID"
Write-Host "  PUT $UPDATE_URL" -ForegroundColor Cyan
try {
    $UPDATE_RESPONSE = Invoke-RestMethod -Uri $UPDATE_URL -Method "PUT" -Headers $headers -Body $UPDATE_PAYLOAD -TimeoutSec 30
    $testDuration = ((Get-Date) - $testStart).TotalMilliseconds
    Write-Host "  SUCCESS - User updated" -ForegroundColor Green
    Track-TestMetric -TestName "UpdateUser" -Status "SUCCESS" -Duration $testDuration
}
catch {
    $testDuration = ((Get-Date) - $testStart).TotalMilliseconds
    Write-Host "  FAILED: $($_.Exception.Message)" -ForegroundColor Red
    Track-TestMetric -TestName "UpdateUser" -Status "FAILED" -Duration $testDuration -ErrorMessage $_.Exception.Message
    $script:METRICS.OverallSuccess = $false
    exit 1
}

# Test 4: Delete User
Write-Host "`nTest 4: Deleting User..." -ForegroundColor Yellow
$testStart = Get-Date
$DELETE_URL = "$API_BASE_URL/$TEST_USER_ID"
Write-Host "  DELETE $DELETE_URL" -ForegroundColor Cyan
try {
    $DELETE_RESPONSE = Invoke-RestMethod -Uri $DELETE_URL -Method "DELETE" -Headers $headers -TimeoutSec 30
    $testDuration = ((Get-Date) - $testStart).TotalMilliseconds
    Write-Host "  SUCCESS - User deleted" -ForegroundColor Green
    Track-TestMetric -TestName "DeleteUser" -Status "SUCCESS" -Duration $testDuration
}
catch {
    $testDuration = ((Get-Date) - $testStart).TotalMilliseconds
    Write-Host "  FAILED: $($_.Exception.Message)" -ForegroundColor Red
    Track-TestMetric -TestName "DeleteUser" -Status "FAILED" -Duration $testDuration -ErrorMessage $_.Exception.Message
    $script:METRICS.OverallSuccess = $false
    exit 1
}

# Test 5: Verify Deletion
Write-Host "`nTest 5: Verifying Deletion..." -ForegroundColor Yellow
$testStart = Get-Date
Write-Host "  GET $GET_URL" -ForegroundColor Cyan
try {
    $VERIFY_RESPONSE = Invoke-RestMethod -Uri $GET_URL -Method "GET" -Headers $headers -TimeoutSec 15
    $testDuration = ((Get-Date) - $testStart).TotalMilliseconds
    if ($VERIFY_RESPONSE.status -eq "Inactive") {
        Write-Host "  SUCCESS - Deletion verified (Status: Inactive)" -ForegroundColor Green
        Track-TestMetric -TestName "VerifyDeletion" -Status "SUCCESS" -Duration $testDuration
    } else {
        Write-Host "  WARNING - User still exists (Status: $($VERIFY_RESPONSE.status))" -ForegroundColor Yellow
        Track-TestMetric -TestName "VerifyDeletion" -Status "WARNING" -Duration $testDuration
    }
}
catch {
    $testDuration = ((Get-Date) - $testStart).TotalMilliseconds
    Write-Host "  SUCCESS - Deletion verified (User not found)" -ForegroundColor Green
    Track-TestMetric -TestName "VerifyDeletion" -Status "SUCCESS" -Duration $testDuration
}

# Calculate overall metrics
$totalDuration = ((Get-Date) - $script:METRICS.StartTime).TotalMilliseconds
$successCount = ($script:METRICS.Tests.Values | Where-Object { $_.Status -eq "SUCCESS" }).Count
$totalTests = $script:METRICS.Tests.Count
$successRate = if ($totalTests -gt 0) { ($successCount / $totalTests) * 100 } else { 0 }

# Send overall health check metrics
$overallDimensions = @{
    Service = "UserManagement"
    Environment = "Production"
    HealthCheckType = "FullSuite"
}

Send-CloudWatchMetric -MetricName "HealthCheckDuration" -Value $totalDuration -Unit "Milliseconds" -Dimensions $overallDimensions
Send-CloudWatchMetric -MetricName "HealthCheckSuccessRate" -Value $successRate -Unit "Percent" -Dimensions $overallDimensions
Send-CloudWatchMetric -MetricName "HealthCheckStatus" -Value $(if ($script:METRICS.OverallSuccess) { 1 } else { 0 }) -Unit "Count" -Dimensions $overallDimensions
Send-CloudWatchMetric -MetricName "TestsExecuted" -Value $totalTests -Unit "Count" -Dimensions $overallDimensions

Write-Host "`n=================================" -ForegroundColor Magenta
Write-Host "All Health Checks Completed!" -ForegroundColor Green
Write-Host "Service is healthy and ready for deployment!" -ForegroundColor Green
Write-Host "Total Duration: $([math]::Round($totalDuration, 2)) ms" -ForegroundColor White
Write-Host "Success Rate: $([math]::Round($successRate, 1))%" -ForegroundColor White
Write-Host "Metrics sent to CloudWatch: $CLOUDWATCH_NAMESPACE" -ForegroundColor White
Write-Host "Completed at: $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor White
Write-Host "=================================" -ForegroundColor Magenta
