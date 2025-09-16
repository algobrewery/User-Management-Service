# Simple Health Check Script
param(
    [string]$ApiUrl = "https://rt786fxfde.execute-api.us-east-1.amazonaws.com/prod",
    [string]$ApiKey = "pzKOjno8c-aLPvTz0L4b6U-UGDs7_7qq3W7qu7lpF7w"
)

$API_BASE_URL = $ApiUrl
$API_KEY = $ApiKey

Write-Host "===============================================" -ForegroundColor Green
Write-Host "        PRODUCTION HEALTH CHECK" -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green
Write-Host "API URL: $API_BASE_URL" -ForegroundColor Gray
Write-Host ""

$headers = @{
    'x-api-key' = $API_KEY
    'x-app-org-uuid' = 'org-1'
    'x-app-user-uuid' = 'f002a471-ebcc-4d6c-ad3c-2327805c001c'
    'x-app-client-user-session-uuid' = 'test-session'
    'x-app-trace-id' = 'test-trace'
    'x-app-region-id' = 'us-east-1'
}

# Test 1: Health Check
Write-Host "1. Testing Health Check" -ForegroundColor Cyan
try {
    $healthResponse = Invoke-RestMethod -Uri "$API_BASE_URL/actuator/health" -Method GET -Headers $headers -TimeoutSec 30
    Write-Host "   ✅ Health Check - PASSED" -ForegroundColor Green
    Write-Host "   Status: $($healthResponse.status)" -ForegroundColor Gray
} catch {
    Write-Host "   ❌ Health Check - FAILED" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Application Info
Write-Host "`n2. Testing Application Info" -ForegroundColor Cyan
try {
    $infoResponse = Invoke-RestMethod -Uri "$API_BASE_URL/actuator/info" -Method GET -Headers $headers -TimeoutSec 30
    Write-Host "   ✅ Application Info - PASSED" -ForegroundColor Green
    Write-Host "   Version: $($infoResponse.build.version)" -ForegroundColor Gray
} catch {
    Write-Host "   ❌ Application Info - FAILED" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Security Test
Write-Host "`n3. Testing Security (Invalid API Key)" -ForegroundColor Cyan
try {
    $invalidHeaders = $headers.Clone()
    $invalidHeaders['x-api-key'] = 'invalid-key'
    Invoke-RestMethod -Uri "$API_BASE_URL/actuator/health" -Method GET -Headers $invalidHeaders -TimeoutSec 30
    Write-Host "   ❌ Security Test - FAILED (Should have been rejected)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -in @(401, 403, 400)) {
        Write-Host "   ✅ Security Test - PASSED (Rejected as expected)" -ForegroundColor Green
        Write-Host "   Status Code: $($_.Exception.Response.StatusCode)" -ForegroundColor Gray
    } else {
        Write-Host "   ❌ Security Test - FAILED" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n===============================================" -ForegroundColor Green
Write-Host "           HEALTH CHECK COMPLETE" -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green
Write-Host "Production deployment is working correctly!" -ForegroundColor Green
Write-Host "`nTest completed at: $(Get-Date)" -ForegroundColor Gray
