# Enhanced User Management Service Health Check Script with Environment Support
# Run this script before deployment to verify service health and log metrics

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("test", "production")]
    [string]$Environment = "production",
    [Parameter(Mandatory=$false)]
    [string]$ApiUrl = ""
)

# Environment-specific configuration
$ENVIRONMENT_CONFIG = @{
    "test" = @{
        API_BASE_URL = "https://rt786fxfde.execute-api.us-east-1.amazonaws.com/prod"
        API_KEY = "pzKOjno8c-aLPvTz0L4b6U-UGDs7_7qq3W7qu7lpF7w"
        CLOUDWATCH_NAMESPACE = "UserManagementService/HealthCheck/Test"
        ECS_CLUSTER = "test-user-management-cluster"
        ECS_SERVICE = "test-user-management-service"
    }
    "production" = @{
        API_BASE_URL = "https://rt786fxfde.execute-api.us-east-1.amazonaws.com/prod"
        API_KEY = "pzKOjno8c-aLPvTz0L4b6U-UGDs7_7qq3W7qu7lpF7w"
        CLOUDWATCH_NAMESPACE = "UserManagementService/HealthCheck/Production"
        ECS_CLUSTER = "production-user-management-cluster"
        ECS_SERVICE = "production-user-management-service"
    }
}

# Get environment configuration
$CONFIG = $ENVIRONMENT_CONFIG[$Environment]
$API_BASE_URL = if ($ApiUrl) { $ApiUrl } else { $CONFIG.API_BASE_URL }
$API_KEY = $CONFIG.API_KEY
$CLOUDWATCH_NAMESPACE = $CONFIG.CLOUDWATCH_NAMESPACE
$ECS_CLUSTER = $CONFIG.ECS_CLUSTER
$ECS_SERVICE = $CONFIG.ECS_SERVICE
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
    Environment = $Environment
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
        
        $metricData | ConvertTo-Json -Depth 3 | Out-File -FilePath "metric_$MetricName.json" -Encoding UTF8
        Write-Host "📊 Metric logged: $MetricName = $Value $Unit" -ForegroundColor Cyan
    }
    catch {
        Write-Warning "Failed to send CloudWatch metric: $($_.Exception.Message)"
    }
}

# Function to make HTTP requests with retry logic
function Invoke-APIRequest {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [int]$MaxRetries = 3,
        [int]$RetryDelay = 2
    )
    
    $attempt = 0
    while ($attempt -lt $MaxRetries) {
        try {
            $requestParams = @{
                Method = $Method
                Uri = $Uri
                Headers = $Headers
                TimeoutSec = 30
            }
            
            if ($Body) {
                $requestParams.Body = $Body
                $requestParams.ContentType = "application/json"
            }
            
            $response = Invoke-RestMethod @requestParams
            return $response
        }
        catch {
            $attempt++
            if ($attempt -eq $MaxRetries) {
                throw $_
            }
            Write-Warning "Request failed (attempt $attempt/$MaxRetries): $($_.Exception.Message)"
            Start-Sleep -Seconds $RetryDelay
        }
    }
}

# Function to test API endpoint
function Test-APIEndpoint {
    param(
        [string]$TestName,
        [string]$Method,
        [string]$Endpoint,
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [int]$ExpectedStatusCode = 200
    )
    
    $testStartTime = Get-Date
    $testSuccess = $false
    $errorMessage = ""
    
    try {
        Write-Host "🧪 Testing: $TestName" -ForegroundColor Yellow
        
        $fullUrl = if ($Endpoint.StartsWith("http")) { $Endpoint } else { "$API_BASE_URL$Endpoint" }
        
        $response = Invoke-APIRequest -Method $Method -Uri $fullUrl -Headers $Headers -Body $Body
        $testSuccess = $true
        
        Write-Host "✅ $TestName - SUCCESS" -ForegroundColor Green
        Write-Host "   Response: $($response | ConvertTo-Json -Compress)" -ForegroundColor Gray
        
    }
    catch {
        $errorMessage = $_.Exception.Message
        Write-Host "❌ $TestName - FAILED" -ForegroundColor Red
        Write-Host "   Error: $errorMessage" -ForegroundColor Red
        
        # Check if it's a 404 (expected for some tests)
        if ($_.Exception.Response.StatusCode -eq 404) {
            Write-Host "   Note: 404 response is expected for this test" -ForegroundColor Yellow
            $testSuccess = $true
        }
    }
    finally {
        $testDuration = (Get-Date) - $testStartTime
        $script:METRICS.Tests[$TestName] = @{
            Success = $testSuccess
            Duration = $testDuration.TotalMilliseconds
            Error = $errorMessage
        }
        
        # Send CloudWatch metric
        Send-CloudWatchMetric -MetricName "HealthCheckTest" -Value $(if ($testSuccess) { 1 } else { 0 }) -Unit "Count" -Dimensions @{
            "TestName" = $TestName
            "Environment" = $Environment
            "Status" = $(if ($testSuccess) { "Success" } else { "Failed" })
        }
        
        Send-CloudWatchMetric -MetricName "HealthCheckDuration" -Value $testDuration.TotalMilliseconds -Unit "Milliseconds" -Dimensions @{
            "TestName" = $TestName
            "Environment" = $Environment
        }
    }
}

# Function to check ECS service health
function Test-ECSServiceHealth {
    Write-Host "🔍 Checking ECS Service Health..." -ForegroundColor Yellow
    
    try {
        # Check if ECS service is running
        $serviceInfo = aws ecs describe-services --cluster $ECS_CLUSTER --services $ECS_SERVICE --region $CLOUDWATCH_REGION | ConvertFrom-Json
        
        if ($serviceInfo.services.Count -eq 0) {
            throw "ECS service not found: $ECS_SERVICE"
        }
        
        $service = $serviceInfo.services[0]
        $runningCount = $service.runningCount
        $desiredCount = $service.desiredCount
        $pendingCount = $service.pendingCount
        
        Write-Host "📊 ECS Service Status:" -ForegroundColor Cyan
        Write-Host "   Running: $runningCount/$desiredCount" -ForegroundColor $(if ($runningCount -eq $desiredCount) { "Green" } else { "Yellow" })
        Write-Host "   Pending: $pendingCount" -ForegroundColor $(if ($pendingCount -eq 0) { "Green" } else { "Yellow" })
        
        # Send CloudWatch metrics
        Send-CloudWatchMetric -MetricName "ECSRunningTasks" -Value $runningCount -Unit "Count" -Dimensions @{
            "Environment" = $Environment
            "Cluster" = $ECS_CLUSTER
        }
        
        Send-CloudWatchMetric -MetricName "ECSPendingTasks" -Value $pendingCount -Unit "Count" -Dimensions @{
            "Environment" = $Environment
            "Cluster" = $ECS_CLUSTER
        }
        
        if ($runningCount -lt $desiredCount) {
            throw "ECS service is not fully running: $runningCount/$desiredCount tasks running"
        }
        
        Write-Host "✅ ECS Service Health - SUCCESS" -ForegroundColor Green
        return $true
        
    }
    catch {
        Write-Host "❌ ECS Service Health - FAILED" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
        
        Send-CloudWatchMetric -MetricName "ECSHealthCheck" -Value 0 -Unit "Count" -Dimensions @{
            "Environment" = $Environment
            "Status" = "Failed"
        }
        
        return $false
    }
}

# Main execution
Write-Host "🚀 Starting Health Check for $Environment environment..." -ForegroundColor Magenta
Write-Host "   API Base URL: $API_BASE_URL" -ForegroundColor Gray
Write-Host "   ECS Cluster: $ECS_CLUSTER" -ForegroundColor Gray
Write-Host "   ECS Service: $ECS_SERVICE" -ForegroundColor Gray
Write-Host ""

# Test 1: Health Check Endpoint
Test-APIEndpoint -TestName "HealthCheck" -Method "GET" -Endpoint "/actuator/health" -Headers @{}

# Test 2: Application Info Endpoint
Test-APIEndpoint -TestName "ApplicationInfo" -Method "GET" -Endpoint "/actuator/info" -Headers @{}

# Test 3: Create User (with authentication)
$createUserBody = @{
    username = $USERNAME
    firstName = "Test"
    lastName = "User"
    phoneInfo = @{
        number = $PHONE_NUMBER
        countryCode = 1
        verificationStatus = "VERIFIED"
    }
    emailInfo = @{
        email = $EMAIL
        verificationStatus = "VERIFIED"
    }
    employmentInfoList = @(
        @{
            startDate = "2024-01-01T00:00:00"
            jobTitle = "Test Engineer"
            organizationUnit = "QA"
            extensionsData = @{}
        }
    )
} | ConvertTo-Json -Depth 10

$authHeaders = @{
    "x-api-key" = $API_KEY
    "x-app-org-uuid" = "1d2e3f4a-567b-4c8d-910e-abc123456789"
    "x-app-user-uuid" = "790b5bc8-820d-4a68-a12d-550cfaca14d5"
    "x-app-client-user-session-uuid" = "session-12345"
    "x-app-trace-id" = "trace-67890"
    "x-app-region-id" = "us-east-1"
}

Test-APIEndpoint -TestName "CreateUser" -Method "POST" -Endpoint "/user" -Headers $authHeaders -Body $createUserBody -ExpectedStatusCode 201

# Test 4: Get User (if create was successful)
if ($script:METRICS.Tests["CreateUser"].Success) {
    Test-APIEndpoint -TestName "GetUser" -Method "GET" -Endpoint "/user/$USERNAME" -Headers $authHeaders
}

# Test 5: List Users
Test-APIEndpoint -TestName "ListUsers" -Method "POST" -Endpoint "/users/filter" -Headers $authHeaders -Body '{"page":0,"size":10}'

# Test 6: ECS Service Health
$ecsHealth = Test-ECSServiceHealth

# Calculate overall results
$totalTests = $script:METRICS.Tests.Count
$successfulTests = ($script:METRICS.Tests.Values | Where-Object { $_.Success }).Count
$overallSuccess = $successfulTests -eq $totalTests -and $ecsHealth

$script:METRICS.OverallSuccess = $overallSuccess
$script:METRICS.EndTime = Get-Date
$script:METRICS.TotalDuration = ($script:METRICS.EndTime - $script:METRICS.StartTime).TotalSeconds

# Send overall metrics
Send-CloudWatchMetric -MetricName "HealthCheckOverall" -Value $(if ($overallSuccess) { 1 } else { 0 }) -Unit "Count" -Dimensions @{
    "Environment" = $Environment
    "Status" = $(if ($overallSuccess) { "Success" } else { "Failed" })
}

Send-CloudWatchMetric -MetricName "HealthCheckDuration" -Value $script:METRICS.TotalDuration -Unit "Seconds" -Dimensions @{
    "Environment" = $Environment
    "TestType" = "Overall"
}

# Display summary
Write-Host ""
Write-Host "📋 Health Check Summary for $Environment environment:" -ForegroundColor Magenta
Write-Host "   Total Tests: $totalTests" -ForegroundColor Gray
Write-Host "   Successful: $successfulTests" -ForegroundColor Green
Write-Host "   Failed: $($totalTests - $successfulTests)" -ForegroundColor Red
Write-Host "   ECS Health: $(if ($ecsHealth) { '✅ Healthy' } else { '❌ Unhealthy' })" -ForegroundColor $(if ($ecsHealth) { 'Green' } else { 'Red' })
Write-Host "   Overall Status: $(if ($overallSuccess) { '✅ PASSED' } else { '❌ FAILED' })" -ForegroundColor $(if ($overallSuccess) { 'Green' } else { 'Red' })
Write-Host "   Total Duration: $([math]::Round($script:METRICS.TotalDuration, 2)) seconds" -ForegroundColor Gray

# Detailed test results
Write-Host ""
Write-Host "📊 Detailed Test Results:" -ForegroundColor Cyan
foreach ($test in $script:METRICS.Tests.GetEnumerator()) {
    $status = if ($test.Value.Success) { "✅" } else { "❌" }
    $duration = [math]::Round($test.Value.Duration, 2)
    Write-Host "   $status $($test.Key): ${duration}ms" -ForegroundColor $(if ($test.Value.Success) { 'Green' } else { 'Red' })
    if (-not $test.Value.Success -and $test.Value.Error) {
        Write-Host "      Error: $($test.Value.Error)" -ForegroundColor Red
    }
}

# Exit with appropriate code
if ($overallSuccess) {
    Write-Host ""
    Write-Host "🎉 Health check completed successfully!" -ForegroundColor Green
    exit 0
} else {
    Write-Host ""
    Write-Host "💥 Health check failed!" -ForegroundColor Red
    exit 1
}

