@echo off
echo 🔍 Testing Database Connections...

REM Test Database Details
set TEST_HOST=nucleus-staging-1.culss6gmw8na.us-east-1.rds.amazonaws.com
set TEST_USER=nucleus_staging
set TEST_PASS=nucleus_staging
set TEST_DB=nucleus_staging_db

REM Production Database Details
set PROD_HOST=user-management-prod-db.culss6gmw8na.us-east-1.rds.amazonaws.com
set PROD_USER=postgres
set PROD_PASS=tasksilodbtest
set PROD_DB=usermanagement_prod

echo.
echo 🧪 Testing TEST Database Connection...
echo Host: %TEST_HOST%
echo Database: %TEST_DB%
echo User: %TEST_USER%

REM Test connectivity using telnet (basic port check)
echo Testing port connectivity...
powershell -Command "try { $tcpClient = New-Object System.Net.Sockets.TcpClient; $tcpClient.Connect('%TEST_HOST%', 5432); $tcpClient.Close(); Write-Host '✅ TEST Database port is reachable' } catch { Write-Host '❌ TEST Database port is not reachable' }"

echo.
echo 🚀 Testing PRODUCTION Database Connection...
echo Host: %PROD_HOST%
echo Database: %PROD_DB%
echo User: %PROD_USER%

REM Test connectivity using telnet (basic port check)
echo Testing port connectivity...
powershell -Command "try { $tcpClient = New-Object System.Net.Sockets.TcpClient; $tcpClient.Connect('%PROD_HOST%', 5432); $tcpClient.Close(); Write-Host '✅ PRODUCTION Database port is reachable' } catch { Write-Host '❌ PRODUCTION Database port is not reachable' }"

echo.
echo 🎯 Database Connection Test Complete!
pause
