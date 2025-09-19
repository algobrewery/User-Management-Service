# Monitor Canary Test Results
# This script helps you view canary test results from GitHub Actions

param(
    [Parameter(Mandatory=$false)]
    [string]$Repository = "sindhuj/User-Management-Service-role",
    
    [Parameter(Mandatory=$false)]
    [int]$Days = 7,
    
    [Parameter(Mandatory=$false)]
    [string]$GitHubToken = $null
)

Write-Host "CANARY TEST RESULTS MONITOR" -ForegroundColor Magenta
Write-Host "===========================" -ForegroundColor Magenta
Write-Host "Repository: $Repository" -ForegroundColor Gray
Write-Host "Time Range: Last $Days days" -ForegroundColor Gray
Write-Host ""

# Check if GitHub CLI is available
try {
    $ghVersion = gh --version 2>$null
    Write-Host "GitHub CLI Version: $ghVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ GitHub CLI is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install GitHub CLI: https://cli.github.com/" -ForegroundColor Red
    Write-Host "Or provide a GitHub token for API access" -ForegroundColor Red
    exit 1
}

# Check authentication
try {
    $authStatus = gh auth status 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Not authenticated with GitHub CLI" -ForegroundColor Red
        Write-Host "Please run: gh auth login" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ GitHub CLI authentication failed" -ForegroundColor Red
    exit 1
}

Write-Host "Fetching canary test results..." -ForegroundColor Yellow

# Get workflow runs for canary tests
try {
    $workflowRuns = gh run list --workflow="ci-cd-pipeline.yml" --repo=$Repository --limit=50 --json databaseId,status,conclusion,createdAt,updatedAt,displayTitle,headBranch 2>$null | ConvertFrom-Json
    
    if (-not $workflowRuns) {
        Write-Host "❌ No workflow runs found" -ForegroundColor Red
        exit 1
    }
    
    # Filter for canary test runs (scheduled runs)
    $canaryRuns = $workflowRuns | Where-Object { 
        $_.displayTitle -match "canary|schedule" -or 
        $_.headBranch -eq "main" -and $_.createdAt -gt (Get-Date).AddDays(-$Days).ToString("yyyy-MM-ddTHH:mm:ssZ")
    }
    
    if (-not $canaryRuns) {
        Write-Host "❌ No canary test runs found in the last $Days days" -ForegroundColor Red
        exit 1
    }
    
} catch {
    Write-Host "❌ Error fetching workflow runs: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Analyze results
$totalRuns = $canaryRuns.Count
$successfulRuns = ($canaryRuns | Where-Object { $_.conclusion -eq "success" }).Count
$failedRuns = ($canaryRuns | Where-Object { $_.conclusion -eq "failure" }).Count
$cancelledRuns = ($canaryRuns | Where-Object { $_.conclusion -eq "cancelled" }).Count
$inProgressRuns = ($canaryRuns | Where-Object { $_.status -eq "in_progress" }).Count

$successRate = if ($totalRuns -gt 0) { [math]::Round(($successfulRuns / $totalRuns) * 100, 2) } else { 0 }

Write-Host "CANARY TEST SUMMARY (Last $Days days)" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Total Runs: $totalRuns" -ForegroundColor White
Write-Host "Successful: $successfulRuns" -ForegroundColor Green
Write-Host "Failed: $failedRuns" -ForegroundColor Red
Write-Host "Cancelled: $cancelledRuns" -ForegroundColor Yellow
Write-Host "In Progress: $inProgressRuns" -ForegroundColor Blue
Write-Host "Success Rate: $successRate%" -ForegroundColor $(if ($successRate -ge 95) { "Green" } elseif ($successRate -ge 90) { "Yellow" } else { "Red" })
Write-Host ""

# Show recent runs
Write-Host "RECENT CANARY TEST RUNS" -ForegroundColor Cyan
Write-Host "=======================" -ForegroundColor Cyan

$recentRuns = $canaryRuns | Sort-Object createdAt -Descending | Select-Object -First 10

foreach ($run in $recentRuns) {
    $runDate = [datetime]::Parse($run.createdAt).ToString("yyyy-MM-dd HH:mm:ss")
    $status = switch ($run.conclusion) {
        "success" { "✅ PASS" }
        "failure" { "❌ FAIL" }
        "cancelled" { "⏹️  CANCELLED" }
        default { "🔄 $($run.status.ToUpper())" }
    }
    
    $statusColor = switch ($run.conclusion) {
        "success" { "Green" }
        "failure" { "Red" }
        "cancelled" { "Yellow" }
        default { "Blue" }
    }
    
    Write-Host "$runDate - $status" -ForegroundColor $statusColor
}

Write-Host ""

# Show detailed logs for failed runs
$failedRuns = $canaryRuns | Where-Object { $_.conclusion -eq "failure" } | Sort-Object createdAt -Descending | Select-Object -First 3

if ($failedRuns) {
    Write-Host "RECENT FAILURES" -ForegroundColor Red
    Write-Host "===============" -ForegroundColor Red
    
    foreach ($run in $failedRuns) {
        $runDate = [datetime]::Parse($run.createdAt).ToString("yyyy-MM-dd HH:mm:ss")
        Write-Host "Failed run: $runDate (ID: $($run.databaseId))" -ForegroundColor Red
        
        # Get logs for failed run
        try {
            Write-Host "  Getting logs..." -ForegroundColor Gray
            $logs = gh run view $run.databaseId --repo=$Repository --log 2>$null
            
            if ($logs) {
                $errorLines = $logs | Where-Object { $_ -match "FAILED|ERROR|❌" }
                if ($errorLines) {
                    Write-Host "  Recent errors:" -ForegroundColor Yellow
                    $errorLines | Select-Object -Last 3 | ForEach-Object {
                        Write-Host "    $_" -ForegroundColor Red
                    }
                }
            }
        } catch {
            Write-Host "  Could not retrieve logs" -ForegroundColor Gray
        }
        Write-Host ""
    }
}

# Show recommendations
Write-Host "RECOMMENDATIONS" -ForegroundColor Cyan
Write-Host "===============" -ForegroundColor Cyan

if ($successRate -lt 95) {
    Write-Host "⚠️  Success rate is below 95%. Consider investigating recent failures." -ForegroundColor Yellow
}

if ($failedRuns -gt 0) {
    Write-Host "🔍 Review failed runs to identify patterns or recurring issues." -ForegroundColor Yellow
}

if ($totalRuns -lt 10) {
    Write-Host "📊 Limited data available. Check back after more runs complete." -ForegroundColor Yellow
}

Write-Host "✅ Canary tests are running automatically every 5 minutes" -ForegroundColor Green
Write-Host "📈 Monitor trends over time to identify service health patterns" -ForegroundColor Green
Write-Host "🔧 Adjust test frequency if needed using configure-canary-tests.ps1" -ForegroundColor Green

Write-Host ""
Write-Host "LINKS" -ForegroundColor Cyan
Write-Host "=====" -ForegroundColor Cyan
Write-Host "GitHub Actions: https://github.com/$Repository/actions" -ForegroundColor White
Write-Host "Workflow Runs: https://github.com/$Repository/actions/workflows/ci-cd-pipeline.yml" -ForegroundColor White

Write-Host ""
Write-Host "To run this monitor again:" -ForegroundColor Gray
Write-Host ".\monitor-canary-results.ps1 -Repository $Repository -Days $Days" -ForegroundColor Gray
