@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ===========================================================
REM Persons Finder — Smoke Test Script (Windows)
REM Usage: test-api.bat [BASE_URL]
REM Default: http://localhost:5000
REM ===========================================================

set BASE_URL=%1
if "%BASE_URL%"=="" set BASE_URL=http://localhost:5000

set PASS=0
set FAIL=0

echo ================================================================
echo   Persons Finder — Smoke Test
echo   Target: %BASE_URL%
echo ================================================================
echo.

REM ==================== 1. Create person ====================
echo 1. POST /persons -- Create person
curl -s -X POST "%BASE_URL%/persons" -H "Content-Type: application/json" -d "{\"name\":\"Test User\",\"jobTitle\":\"Software Engineer\",\"hobbies\":[\"hiking\",\"chess\"],\"latitude\":40.7128,\"longitude\":-74.006}" > %TEMP%\test1.json
findstr "id" %TEMP%\test1.json >nul && (echo   [PASS] Created & set /a PASS+=1) || (echo   [FAIL] Create failed & set /a FAIL+=1)
echo.

REM Extract ID
for /f "tokens=2 delims=:," %%a in ('findstr "id" %TEMP%\test1.json') do set PID=%%a
echo   Person ID: %PID%
echo.

REM ==================== 2. List all IDs ====================
echo 2. GET /persons -- List all IDs
curl -s "%BASE_URL%/persons" > %TEMP%\test2.json
findstr "%PID%" %TEMP%\test2.json >nul && (echo   [PASS] ID found & set /a PASS+=1) || (echo   [FAIL] ID not found & set /a FAIL+=1)
echo.

REM ==================== 3. Get person ====================
echo 3. GET /persons/{id} -- Get person details
curl -s "%BASE_URL%/persons/%PID%" > %TEMP%\test3.json
findstr "Test User" %TEMP%\test3.json >nul && (echo   [PASS] Detail fetched & set /a PASS+=1) || (echo   [FAIL] Fetch failed & set /a FAIL+=1)
echo.

REM ==================== 4. Update location ====================
echo 4. PUT /persons/{id}/location -- Update location
curl -s -X PUT "%BASE_URL%/persons/%PID%/location" -H "Content-Type: application/json" -d "{\"latitude\":34.0522,\"longitude\":-118.2437}" > %TEMP%\test4.json
findstr "34.0522" %TEMP%\test4.json >nul && (echo   [PASS] Location updated & set /a PASS+=1) || (echo   [FAIL] Update failed & set /a FAIL+=1)
echo.

REM ==================== 5. Nearby search ====================
echo 5. GET /persons/nearby -- Nearby search
curl -s "%BASE_URL%/persons/nearby?latitude=40.7&longitude=-74.0&count=3" > %TEMP%\test5.json
findstr "data" %TEMP%\test5.json >nul && (echo   [PASS] Nearby search works & set /a PASS+=1) || (echo   [FAIL] Search failed & set /a FAIL+=1)
echo.

REM ==================== 6. Seed data ====================
echo 6. GET /persons/seed -- Seed data
curl -s "%BASE_URL%/persons/seed?count=100" > %TEMP%\test6.json
findstr "inserted" %TEMP%\test6.json >nul && (echo   [PASS] Seed succeeded & set /a PASS+=1) || (echo   [FAIL] Seed failed & set /a FAIL+=1)
echo.

REM ==================== 7. 404 error ====================
echo 7. Error handling -- Non-existent person
curl -s -o %TEMP%\test7.json -w "%%{http_code}" "%BASE_URL%/persons/99999" > %TEMP%\test7_code.txt
set /p HTTP_CODE=<%TEMP%\test7_code.txt
if "!HTTP_CODE!"=="404" (echo   [PASS] Returns 404 & set /a PASS+=1) else (echo   [FAIL] Expected 404, got !HTTP_CODE! & set /a FAIL+=1)
echo.

REM ==================== 8. 400 error ====================
echo 8. Error handling -- Invalid params
curl -s -o %TEMP%\test8.json -w "%%{http_code}" -X POST "%BASE_URL%/persons" -H "Content-Type: application/json" -d "{\"name\":\"\",\"jobTitle\":\"Dev\",\"hobbies\":[],\"latitude\":0,\"longitude\":0}" > %TEMP%\test8_code.txt
set /p HTTP_CODE=<%TEMP%\test8_code.txt
if "!HTTP_CODE!"=="400" (echo   [PASS] Empty name returns 400 & set /a PASS+=1) else (echo   [FAIL] Expected 400, got !HTTP_CODE! & set /a FAIL+=1)
echo.

REM ==================== 9. Infrastructure ====================
echo 9. Infrastructure
curl -s -o nul -w "%%{http_code}" "%BASE_URL%/swagger-ui/index.html" > %TEMP%\test9_swagger.txt
set /p HTTP_SW=<%TEMP%\test9_swagger.txt
if "!HTTP_SW!"=="200" (echo   [PASS] Swagger UI reachable & set /a PASS+=1) else (echo   [FAIL] Swagger unreachable & set /a FAIL+=1)

curl -s -o nul -w "%%{http_code}" "%BASE_URL%/" > %TEMP%\test9_root.txt
set /p HTTP_ROOT=<%TEMP%\test9_root.txt
if "!HTTP_ROOT!"=="302" (echo   [PASS] Root redirects to Swagger & set /a PASS+=1) else (echo   [FAIL] Root not redirected & set /a FAIL+=1)
echo.

REM ==================== Summary ====================
echo ================================================================
echo   Test Complete
echo   Passed: %PASS%
if %FAIL% gtr 0 (
    echo   Failed: %FAIL%
    echo ================================================================
    exit /b 1
) else (
    echo   All passed!
    echo ================================================================
    exit /b 0
)
