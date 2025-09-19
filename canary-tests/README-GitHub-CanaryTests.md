# GitHub Actions Canary Tests for User Management Service

This document describes the canary testing implementation using GitHub Actions for continuous monitoring of the User Management Service in production.

## Overview

Canary tests are lightweight, automated tests that run continuously in production to verify that the service is healthy and responding correctly. They help detect issues before they impact users.

## Architecture

```
GitHub Actions (Schedule) → Production API → Test Results → GitHub Actions Logs
```

### Key Features

- **⚡ Lightweight Tests**: Only 4 essential health checks
- **🕐 Configurable Scheduling**: Runs every 1-60 minutes (default: 5 minutes)
- **💰 Cost Effective**: Uses GitHub Actions free tier
- **📊 Real-time Monitoring**: Results logged in GitHub Actions
- **🚨 Automatic Alerts**: Failed tests trigger workflow failures
- **🔧 Easy Configuration**: Simple PowerShell scripts for management

## Implementation

### Files Added/Modified

1. **`.github/workflows/ci-cd-pipeline.yml`** - Added canary test job
2. **`canary-config.yml`** - Configuration file for test settings
3. **`configure-canary-tests.ps1`** - Script to change test frequency
4. **`monitor-canary-results.ps1`** - Script to view test results

### Canary Test Job

The canary tests are integrated into your existing CI/CD pipeline as a new job that:

- **Runs on Schedule**: Every 5 minutes (configurable)
- **Runs on Manual Trigger**: Via `workflow_dispatch`
- **Skips on Push/PR**: Only runs when scheduled or manually triggered
- **Tests Production**: Uses your production API endpoints

## Canary Tests

The canary tests perform these lightweight checks:

1. **Health Check** (`/actuator/health`) - Basic service health
2. **Application Info** (`/actuator/info`) - Service metadata
3. **System Roles** (`/role/bootstrap/system-managed`) - Database connectivity
4. **List Users** (`/users/filter`) - Core functionality

### Test Configuration

```yaml
# From canary-config.yml
endpoints:
  - name: "Health Check"
    method: "GET"
    path: "/actuator/health"
    expected_status: 200
    critical: true
  
  - name: "Application Info"
    method: "GET"
    path: "/actuator/info"
    expected_status: 200
    critical: false
  
  - name: "System Roles"
    method: "GET"
    path: "/role/bootstrap/system-managed"
    expected_status: 200
    critical: true
  
  - name: "List Users"
    method: "POST"
    path: "/users/filter"
    expected_status: 200
    critical: false
```

## Configuration

### Changing Test Frequency

Use the configuration script to easily change how often tests run:

```powershell
# Set to 10-minute intervals
.\configure-canary-tests.ps1 -IntervalMinutes 10

# Set to 1-minute intervals (high frequency)
.\configure-canary-tests.ps1 -IntervalMinutes 1

# Set to 30-minute intervals (low frequency)
.\configure-canary-tests.ps1 -IntervalMinutes 30
```

### Available Intervals

- **1 minute**: High frequency monitoring (288 runs/day)
- **2 minutes**: Very frequent monitoring (144 runs/day)
- **3 minutes**: Frequent monitoring (96 runs/day)
- **4 minutes**: Regular monitoring (72 runs/day)
- **5 minutes**: Default monitoring (60 runs/day)
- **10 minutes**: Moderate monitoring (30 runs/day)
- **15 minutes**: Light monitoring (20 runs/day)
- **30 minutes**: Minimal monitoring (10 runs/day)
- **60 minutes**: Hourly monitoring (5 runs/day)

## Monitoring

### View Test Results

```powershell
# View last 7 days of results
.\monitor-canary-results.ps1

# View last 3 days of results
.\monitor-canary-results.ps1 -Days 3
```

### GitHub Actions Dashboard

1. Go to your repository on GitHub
2. Click on "Actions" tab
3. Select "CI/CD Pipeline - User Management Service"
4. Look for runs with "Canary Tests - Production Monitoring"

### Test Results Format

```
🧪 Running canary tests...
==========================
Testing: Health Check
  URL: https://rt786fxfde.execute-api.us-east-1.amazonaws.com/prod/actuator/health
  ✅ PASSED (245ms)

Testing: Application Info
  URL: https://rt786fxfde.execute-api.us-east-1.amazonaws.com/prod/actuator/info
  ✅ PASSED (189ms)

Testing: System Roles
  URL: https://rt786fxfde.execute-api.us-east-1.amazonaws.com/prod/role/bootstrap/system-managed
  ✅ PASSED (312ms)

Testing: List Users
  URL: https://rt786fxfde.execute-api.us-east-1.amazonaws.com/prod/users/filter
  ✅ PASSED (456ms)

📊 CANARY TEST RESULTS
=====================
Total Tests: 4
Passed: 4
Failed: 0
Success Rate: 100%
Duration: 2s
Timestamp: 2024-01-15 14:30:00 UTC

🎉 ALL CANARY TESTS PASSED! Production service is healthy.
```

## Cost Analysis

### GitHub Actions Usage

- **Free Tier**: 2,000 minutes/month for private repos
- **Public Repos**: Unlimited minutes
- **Cost**: $0.008/minute for private repos after free tier

### Cost Estimates

| Interval | Runs/Day | Runs/Month | Free Tier Usage | Cost/Month |
|----------|----------|------------|-----------------|------------|
| 1 min    | 1,440    | 43,200     | 2,160 min       | $328.32    |
| 2 min    | 720      | 21,600     | 1,080 min       | $164.16    |
| 3 min    | 480      | 14,400     | 720 min         | $109.44    |
| 4 min    | 360      | 10,800     | 540 min         | $82.08     |
| 5 min    | 288      | 8,640      | 432 min         | $65.66     |
| 10 min   | 144      | 4,320      | 216 min         | $32.83     |
| 15 min   | 96       | 2,880      | 144 min         | $21.89     |
| 30 min   | 48       | 1,440      | 72 min          | $10.94     |
| 60 min   | 24       | 720        | 36 min          | $5.47      |

### Recommendations

- **Production**: Use 10-15 minute intervals for cost-effective monitoring
- **Development**: Use 5-minute intervals for active development
- **Critical Systems**: Use 1-3 minute intervals for high-availability systems

## Alerting

### GitHub Actions Notifications

- **Failed Tests**: Workflow shows as failed (red X)
- **Email Notifications**: Configure in GitHub repository settings
- **Slack Integration**: Available via webhook (see configuration)

### Setting Up Slack Notifications

1. Create a Slack webhook URL
2. Add `SLACK_WEBHOOK_URL` to repository secrets
3. Uncomment the Slack notification code in the workflow

```yaml
# In the workflow file
- name: Send Canary Test Results to Slack (Optional)
  if: always()
  run: |
    if [ -n "${{ secrets.SLACK_WEBHOOK_URL }}" ]; then
      curl -X POST -H 'Content-type: application/json' \
        --data "{\"text\":\"🔍 Canary Test Results: ${{ steps.canary-tests.outputs.canary-status }} - Success Rate: ${{ steps.canary-tests.outputs.canary-success-rate }}%\"}" \
        ${{ secrets.SLACK_WEBHOOK_URL }}
    fi
```

## Troubleshooting

### Common Issues

1. **Tests Failing**: Check API endpoint availability and authentication
2. **High Response Times**: Monitor service performance and database connectivity
3. **Missing Runs**: Verify GitHub Actions is enabled and schedule is correct

### Debugging

1. Check GitHub Actions logs for detailed error messages
2. Use the monitoring script to view recent results
3. Verify API endpoints are accessible manually

### Manual Testing

```bash
# Test endpoints manually
curl -H "x-api-key: pzKOjno8c-aLPvTz0L4b6U-UGDs7_7qq3W7qu7lpF7w" \
     -H "x-app-org-uuid: cts" \
     -H "x-app-user-uuid: 42388507-ec8f-47ef-a7c7-8ddb69763ac6" \
     https://rt786fxfde.execute-api.us-east-1.amazonaws.com/prod/actuator/health
```

## Security

- **API Key**: Stored in workflow (consider using secrets for production)
- **Authentication**: Uses existing API key authentication
- **Permissions**: Minimal GitHub Actions permissions
- **Data**: No sensitive data is logged

## Maintenance

### Regular Tasks

1. **Monitor Success Rates**: Check weekly for trends
2. **Review Failed Tests**: Investigate patterns in failures
3. **Adjust Frequency**: Optimize based on usage patterns
4. **Update Endpoints**: Modify tests if API changes

### Updates

To update canary tests:

1. Modify the test endpoints in the workflow file
2. Update the configuration file if needed
3. Commit and push changes
4. Tests will automatically use new configuration

## Best Practices

1. **Keep Tests Lightweight**: Use minimal payloads and short timeouts
2. **Test Critical Paths**: Focus on essential functionality
3. **Monitor Trends**: Watch for gradual degradation
4. **Set Appropriate Thresholds**: Balance sensitivity with noise
5. **Document Failures**: Keep records of known issues and resolutions

## Integration with CI/CD

The canary tests are integrated into your existing CI/CD pipeline:

- **Deployment Trigger**: Runs after successful deployments
- **Health Check**: Validates production service health
- **Rollback Trigger**: Can be used to trigger rollbacks on failures

## Comparison with AWS Lambda Approach

| Feature | GitHub Actions | AWS Lambda |
|---------|----------------|------------|
| **Cost** | Free tier available | ~$0.50-1.00/month |
| **Setup** | Integrated with CI/CD | Separate infrastructure |
| **Monitoring** | GitHub Actions logs | CloudWatch metrics |
| **Scheduling** | Cron expressions | EventBridge rules |
| **Scaling** | Automatic | Manual configuration |
| **Maintenance** | Minimal | Requires AWS management |

## Support

For issues with canary tests:

1. Check GitHub Actions logs for error details
2. Verify service endpoints are accessible
3. Review workflow configuration
4. Test endpoints manually to isolate issues

## Next Steps

1. **Deploy**: Commit and push the changes to enable canary tests
2. **Monitor**: Watch the first few test runs to ensure they're working
3. **Configure**: Adjust test frequency based on your needs
4. **Alert**: Set up notifications for failed tests
5. **Optimize**: Fine-tune based on monitoring results

The canary tests will start running automatically once you push the changes to your repository. They provide continuous monitoring of your production service with minimal cost and maintenance overhead.
