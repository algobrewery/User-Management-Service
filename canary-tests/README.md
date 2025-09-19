# Canary Tests - User Management Service

This folder contains all the tools and configuration files for monitoring the User Management Service in production using GitHub Actions.

## 📁 Files in this folder:

- **`configure-canary-tests.ps1`** - Script to change test frequency
- **`monitor-canary-results.ps1`** - Script to view test results
- **`canary-config.yml`** - Configuration file for test settings
- **`README-GitHub-CanaryTests.md`** - Detailed documentation

## 🚀 Quick Start:

### 1. Change Test Frequency
```powershell
# Run from the canary-tests folder
.\configure-canary-tests.ps1 -IntervalMinutes 10
```

### 2. Monitor Results
```powershell
# View test results from last 7 days
.\monitor-canary-results.ps1
```

### 3. View Detailed Documentation
Open `README-GitHub-CanaryTests.md` for complete documentation.

## 📊 Current Configuration:
- **Interval**: 30 minutes
- **Tests**: 4 health checks
- **Cost**: FREE (within GitHub Actions free tier)
- **Status**: Active monitoring

## 🔧 Usage from Project Root:
If you're running scripts from the project root directory:

```powershell
# Change frequency
.\canary-tests\configure-canary-tests.ps1 -IntervalMinutes 15

# Monitor results
.\canary-tests\monitor-canary-results.ps1
```

## 📈 What the Canary Tests Monitor:
1. **Health Check** (`/actuator/health`) - Service availability
2. **Application Info** (`/actuator/info`) - Service metadata
3. **System Roles** (`/role/bootstrap/system-managed`) - Database connectivity
4. **List Users** (`/users/filter`) - Core functionality

## 🎯 Benefits:
- ✅ Continuous production monitoring
- ✅ Early issue detection
- ✅ Cost-effective (free with GitHub Actions)
- ✅ Easy to configure and monitor
- ✅ Integrated with your CI/CD pipeline
