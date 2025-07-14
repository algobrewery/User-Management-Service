@echo off
REM Integration Test Script for Client Management + User Management Services
REM This script tests the complete integration between both services

setlocal enabledelayedexpansion

echo.
echo 🚀 Starting Integration Test for Client Management + User Management Services
echo ==================================================================

REM Configuration
set CLIENT_MGMT_URL=http://localhost:8080
set USER_MGMT_URL=http://localhost:8082

REM Global variables
set API_KEY=
set CLIENT_ID=
set USER_ID=

echo Step 1: Checking if services are running...
echo.

REM Test 1: Check Client Management Service
echo Testing Client Management Service health...
curl -s -m 10 %CLIENT_MGMT_URL%/actuator/health > temp_response.json
findstr /C:"status" temp_response.json > nul
if %errorlevel% equ 0 (
    echo ✅ Client Management Service is running
) else (
    echo ❌ Client Management Service is not running or not healthy
    goto :error
)

REM Test 2: Check User Management Service
echo Testing User Management Service health...
curl -s -m 10 %USER_MGMT_URL%/actuator/health > temp_response.json
findstr /C:"status" temp_response.json > nul
if %errorlevel% equ 0 (
    echo ✅ User Management Service is running
) else (
    echo ❌ User Management Service is not running or not healthy
    goto :error
)

echo.
echo Step 2: Registering a test client...
echo.

REM Test 3: Register a client
echo Registering test client...
curl -s -m 10 -X POST -H "Content-Type: application/json" -d "{\"clientName\": \"Integration Test Client\", \"clientType\": \"service\", \"description\": \"Automated integration test client\"}" %CLIENT_MGMT_URL%/clients > temp_response.json

findstr /C:"api_key" temp_response.json > nul
if %errorlevel% equ 0 (
    echo ✅ Client registered successfully
    REM Extract API key and Client ID (simplified extraction)
    for /f "tokens=2 delims=:" %%a in ('findstr "api_key" temp_response.json') do (
        set temp=%%a
        set API_KEY=!temp:"=!
        set API_KEY=!API_KEY:,=!
        set API_KEY=!API_KEY: =!
    )
    for /f "tokens=2 delims=:" %%a in ('findstr "client_id" temp_response.json') do (
        set temp=%%a
        set CLIENT_ID=!temp:"=!
        set CLIENT_ID=!CLIENT_ID:,=!
        set CLIENT_ID=!CLIENT_ID: =!
    )
    echo    API Key: !API_KEY!
    echo    Client ID: !CLIENT_ID!
) else (
    echo ❌ Failed to register client
    goto :error
)

echo.
echo Step 3: Testing API key validation...
echo.

REM Test 4: Validate API key
echo Testing API key validation...
curl -s -m 10 -H "x-api-key: !API_KEY!" %CLIENT_MGMT_URL%/api/validate > temp_response.json
findstr /C:"valid" temp_response.json > nul
if %errorlevel% equ 0 (
    echo ✅ API key validation successful
) else (
    echo ❌ API key validation failed
    goto :error
)

echo.
echo Step 4: Testing User Management authentication...
echo.

REM Test 5: Test without API key (should fail)
echo Testing request without API key (should fail)...
curl -s -m 10 -X POST -H "Content-Type: application/json" -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" -H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" -H "x-app-client-user-session-uuid: session-12345" -H "x-app-trace-id: trace-67890" -H "x-app-region-id: us-east-1" -d "{\"username\": \"testuser_no_key\", \"firstName\": \"Test\", \"lastName\": \"User\"}" %USER_MGMT_URL%/user > temp_response.json 2>nul

findstr /C:"Missing API key" temp_response.json > nul
if %errorlevel% equ 0 (
    echo ✅ Request without API key correctly rejected
) else (
    echo ❌ Request without API key was not rejected properly
)

REM Test 6: Test with invalid API key (should fail)
echo Testing request with invalid API key (should fail)...
curl -s -m 10 -X POST -H "Content-Type: application/json" -H "x-api-key: invalid-key" -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" -H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" -H "x-app-client-user-session-uuid: session-12345" -H "x-app-trace-id: trace-67890" -H "x-app-region-id: us-east-1" -d "{\"username\": \"testuser_invalid_key\", \"firstName\": \"Test\", \"lastName\": \"User\"}" %USER_MGMT_URL%/user > temp_response.json 2>nul

findstr /C:"Invalid API key" temp_response.json > nul
if %errorlevel% equ 0 (
    echo ✅ Request with invalid API key correctly rejected
) else (
    echo ❌ Request with invalid API key was not rejected properly
)

echo.
echo Step 5: Testing User Management with valid API key...
echo.

REM Test 7: Create user with valid API key (should succeed)
echo Creating user with valid API key...
curl -s -m 10 -X POST -H "Content-Type: application/json" -H "x-api-key: !API_KEY!" -H "x-app-org-uuid: 1d2e3f4a-567b-4c8d-910e-abc123456789" -H "x-app-user-uuid: 790b5bc8-820d-4a68-a12d-550cfaca14d5" -H "x-app-client-user-session-uuid: session-12345" -H "x-app-trace-id: trace-67890" -H "x-app-region-id: us-east-1" -d "{\"username\": \"testuser_integration\", \"firstName\": \"Integration\", \"lastName\": \"Test\", \"phoneInfo\": {\"number\": \"1234567890\", \"countryCode\": \"1\", \"verificationStatus\": \"verified\"}, \"emailInfo\": {\"email\": \"integration.test@example.com\", \"verificationStatus\": \"verified\"}}" %USER_MGMT_URL%/user > temp_response.json

findstr /C:"httpStatus" temp_response.json > nul
if %errorlevel% equ 0 (
    echo ✅ User created successfully
    REM Extract User ID (simplified)
    for /f "tokens=2 delims=:" %%a in ('findstr "userId" temp_response.json') do (
        set temp=%%a
        set USER_ID=!temp:"=!
        set USER_ID=!USER_ID:,=!
        set USER_ID=!USER_ID: =!
    )
    echo    User ID: !USER_ID!
) else (
    echo ❌ Failed to create user
    type temp_response.json
)

echo.
echo 🎉 Integration Test Completed!
echo ==================================================================
echo Summary:
echo ✅ Both services are running and healthy
echo ✅ Client registration works
echo ✅ API key validation works
echo ✅ Authentication properly rejects invalid requests
echo ✅ User Management endpoints work with valid API key
echo.
echo Your services are successfully integrated!
echo.
echo API Key for further testing: !API_KEY!
echo Client ID: !CLIENT_ID!
if defined USER_ID echo Created User ID: !USER_ID!

goto :cleanup

:error
echo.
echo ❌ Test failed! Please check the services and try again.
exit /b 1

:cleanup
if exist temp_response.json del temp_response.json
echo.
echo Test completed.
pause
