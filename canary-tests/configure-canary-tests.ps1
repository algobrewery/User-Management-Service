# Configure Canary Test Interval
# This script helps you easily change the canary test frequency

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet(1, 2, 3, 4, 5, 10, 15, 30, 60)]
    [int]$IntervalMinutes = 5,
    
    [Parameter(Mandatory=$false)]
    [string]$GitHubToken = $null
)

Write-Host "CONFIGURE CANARY TESTS" -ForegroundColor Magenta
Write-Host "=====================" -ForegroundColor Magenta
Write-Host ""

# Validate interval
if ($IntervalMinutes -notin @(1, 2, 3, 4, 5, 10, 15, 30, 60)) {
    Write-Host "❌ Invalid interval. Must be one of: 1, 2, 3, 4, 5, 10, 15, 30, 60 minutes" -ForegroundColor Red
    exit 1
}

Write-Host "Setting canary test interval to: $IntervalMinutes minutes" -ForegroundColor Yellow
Write-Host ""

# Convert minutes to cron expression
$cronExpression = switch ($IntervalMinutes) {
    1 { "*/1 * * * *" }
    2 { "*/2 * * * *" }
    3 { "*/3 * * * *" }
    4 { "*/4 * * * *" }
    5 { "*/5 * * * *" }
    10 { "*/10 * * * *" }
    15 { "*/15 * * * *" }
    30 { "*/30 * * * *" }
    60 { "0 * * * *" }
}

Write-Host "Cron expression: $cronExpression" -ForegroundColor Gray
Write-Host ""

# Update the workflow file
$workflowFile = "../.github/workflows/ci-cd-pipeline.yml"

if (-not (Test-Path $workflowFile)) {
    Write-Host "❌ Workflow file not found: $workflowFile" -ForegroundColor Red
    exit 1
}

Write-Host "Updating workflow file..." -ForegroundColor Yellow

# Read the current workflow content
$content = Get-Content $workflowFile -Raw

# Update the cron expression
$pattern = 'cron: ''\*/?\d+ \* \* \* \*'''
$replacement = "cron: '$cronExpression'"

if ($content -match $pattern) {
    $newContent = $content -replace $pattern, $replacement
    Set-Content $workflowFile -Value $newContent -NoNewline
    Write-Host "✅ Workflow file updated successfully!" -ForegroundColor Green
} else {
    Write-Host "❌ Could not find cron expression in workflow file" -ForegroundColor Red
    exit 1
}

# Update the config file
$configFile = "canary-config.yml"
if (Test-Path $configFile) {
    $configContent = Get-Content $configFile -Raw
    $configPattern = 'canary_interval_minutes: \d+'
    $configReplacement = "canary_interval_minutes: $IntervalMinutes"
    
    if ($configContent -match $configPattern) {
        $newConfigContent = $configContent -replace $configPattern, $configReplacement
        Set-Content $configFile -Value $newConfigContent -NoNewline
        Write-Host "✅ Config file updated successfully!" -ForegroundColor Green
    }
}

# Calculate estimated costs
$runsPerDay = [math]::Round(1440 / $IntervalMinutes)
$runsPerMonth = $runsPerDay * 30
$estimatedCost = [math]::Round($runsPerMonth * 0.01, 2)  # Rough estimate

Write-Host ""
Write-Host "CONFIGURATION SUMMARY" -ForegroundColor Cyan
Write-Host "=====================" -ForegroundColor Cyan
Write-Host "Interval: $IntervalMinutes minutes" -ForegroundColor White
Write-Host "Runs per day: $runsPerDay" -ForegroundColor White
Write-Host "Runs per month: $runsPerMonth" -ForegroundColor White
Write-Host "Estimated cost: ~$$estimatedCost/month" -ForegroundColor White
Write-Host ""

# Show next steps
Write-Host "NEXT STEPS" -ForegroundColor Cyan
Write-Host "==========" -ForegroundColor Cyan
Write-Host "1. Commit and push the changes:" -ForegroundColor White
Write-Host "   git add .github/workflows/ci-cd-pipeline.yml canary-tests/canary-config.yml" -ForegroundColor Gray
Write-Host "   git commit -m 'Configure canary tests to run every $IntervalMinutes minutes'" -ForegroundColor Gray
Write-Host "   git push origin main" -ForegroundColor Gray
Write-Host ""
Write-Host "2. The canary tests will start running automatically" -ForegroundColor White
Write-Host "3. Monitor results in GitHub Actions tab" -ForegroundColor White
Write-Host "4. To run tests manually, use 'workflow_dispatch' trigger" -ForegroundColor White
Write-Host ""

# Show current status
Write-Host "CURRENT STATUS" -ForegroundColor Cyan
Write-Host "==============" -ForegroundColor Cyan
Write-Host "✅ Canary tests configured to run every $IntervalMinutes minutes" -ForegroundColor Green
Write-Host "✅ Tests will monitor production service health" -ForegroundColor Green
Write-Host "✅ Results will be logged in GitHub Actions" -ForegroundColor Green
Write-Host "✅ Cost-optimized for continuous monitoring" -ForegroundColor Green
Write-Host ""

if ($IntervalMinutes -le 5) {
    Write-Host "⚠️  WARNING: Running tests every $IntervalMinutes minutes will generate more GitHub Actions usage" -ForegroundColor Yellow
    Write-Host "   Consider using 10-15 minutes for production monitoring" -ForegroundColor Yellow
}

Write-Host "🎉 Canary tests configuration complete!" -ForegroundColor Green
