@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ===========================================================
REM Persons Finder — API 冒烟测试脚本 (Windows 版)
REM 测试人员直接用，双击运行即可
REM 用法: test-api.bat [BASE_URL]
REM 默认: http://localhost:5000
REM ===========================================================

set BASE_URL=%1
if "%BASE_URL%"=="" set BASE_URL=http://localhost:5000

set PASS=0
set FAIL=0

echo ================================================================
echo   Persons Finder — API 冒烟测试
echo   目标: %BASE_URL%
echo ================================================================
echo.

REM ==================== 1. 创建人员 ====================
echo 1. POST /persons -- 创建人员
curl -s -X POST "%BASE_URL%/persons" -H "Content-Type: application/json" -d "{\"name\":\"Test User\",\"jobTitle\":\"Software Engineer\",\"hobbies\":[\"hiking\",\"chess\"],\"latitude\":40.7128,\"longitude\":-74.006}" > %TEMP%\test1.json
findstr "id" %TEMP%\test1.json >nul && (echo   [PASS] 创建成功 & set /a PASS+=1) || (echo   [FAIL] 创建失败 & set /a FAIL+=1)
echo.

REM 提取 ID
for /f "tokens=2 delims=:," %%a in ('findstr "id" %TEMP%\test1.json') do set PID=%%a
echo   人员 ID: %PID%
echo.

REM ==================== 2. 获取所有人员 ID ====================
echo 2. GET /persons -- 获取所有人 ID
curl -s "%BASE_URL%/persons" > %TEMP%\test2.json
findstr "%PID%" %TEMP%\test2.json >nul && (echo   [PASS] 返回 ID 列表 & set /a PASS+=1) || (echo   [FAIL] 未找到 ID & set /a FAIL+=1)
echo.

REM ==================== 3. 获取单个人员 ====================
echo 3. GET /persons/{id} -- 获取人员详情
curl -s "%BASE_URL%/persons/%PID%" > %TEMP%\test3.json
findstr "Test User" %TEMP%\test3.json >nul && (echo   [PASS] 获取详情成功 & set /a PASS+=1) || (echo   [FAIL] 获取失败 & set /a FAIL+=1)
echo.

REM ==================== 4. 更新位置 ====================
echo 4. PUT /persons/{id}/location -- 更新位置
curl -s -X PUT "%BASE_URL%/persons/%PID%/location" -H "Content-Type: application/json" -d "{\"latitude\":34.0522,\"longitude\":-118.2437}" > %TEMP%\test4.json
findstr "34.0522" %TEMP%\test4.json >nul && (echo   [PASS] 更新位置成功 & set /a PASS+=1) || (echo   [FAIL] 更新失败 & set /a FAIL+=1)
echo.

REM ==================== 5. 附近搜索 ====================
echo 5. GET /persons/nearby -- 附近搜索
curl -s "%BASE_URL%/persons/nearby?latitude=40.7&longitude=-74.0&count=3" > %TEMP%\test5.json
findstr "data" %TEMP%\test5.json >nul && (echo   [PASS] 附近搜索成功 & set /a PASS+=1) || (echo   [FAIL] 搜索失败 & set /a FAIL+=1)
echo.

REM ==================== 6. 批量造数据 ====================
echo 6. GET /persons/seed -- 批量造数据
curl -s "%BASE_URL%/persons/seed?count=100" > %TEMP%\test6.json
findstr "inserted" %TEMP%\test6.json >nul && (echo   [PASS] 批量插入成功 & set /a PASS+=1) || (echo   [FAIL] 批量插入失败 & set /a FAIL+=1)
echo.

REM ==================== 7. 404 错误 ====================
echo 7. 错误处理 -- 不存在的人员
curl -s -o %TEMP%\test7.json -w "%%{http_code}" "%BASE_URL%/persons/99999" > %TEMP%\test7_code.txt
set /p HTTP_CODE=<%TEMP%\test7_code.txt
if "!HTTP_CODE!"=="404" (echo   [PASS] 返回 404 & set /a PASS+=1) else (echo   [FAIL] 期望 404, 实际 !HTTP_CODE! & set /a FAIL+=1)
echo.

REM ==================== 8. 400 错误 ====================
echo 8. 错误处理 -- 无效参数
curl -s -o %TEMP%\test8.json -w "%%{http_code}" -X POST "%BASE_URL%/persons" -H "Content-Type: application/json" -d "{\"name\":\"\",\"jobTitle\":\"Dev\",\"hobbies\":[],\"latitude\":0,\"longitude\":0}" > %TEMP%\test8_code.txt
set /p HTTP_CODE=<%TEMP%\test8_code.txt
if "!HTTP_CODE!"=="400" (echo   [PASS] 空名称返回 400 & set /a PASS+=1) else (echo   [FAIL] 期望 400, 实际 !HTTP_CODE! & set /a FAIL+=1)
echo.

REM ==================== 9. 基础设施 ====================
echo 9. 基础设施
curl -s -o nul -w "%%{http_code}" "%BASE_URL%/swagger-ui/index.html" > %TEMP%\test9_swagger.txt
set /p HTTP_SW=<%TEMP%\test9_swagger.txt
if "!HTTP_SW!"=="200" (echo   [PASS] Swagger UI 可达 & set /a PASS+=1) else (echo   [FAIL] Swagger 不可达 & set /a FAIL+=1)

curl -s -o nul -w "%%{http_code}" "%BASE_URL%/" > %TEMP%\test9_root.txt
set /p HTTP_ROOT=<%TEMP%\test9_root.txt
if "!HTTP_ROOT!"=="302" (echo   [PASS] 根路径重定向到 Swagger & set /a PASS+=1) else (echo   [FAIL] 根路径未重定向 & set /a FAIL+=1)
echo.

REM ==================== 总结 ====================
echo ================================================================
echo   测试完成
echo   通过: %PASS%
if %FAIL% gtr 0 (
    echo   失败: %FAIL%
    echo ================================================================
    exit /b 1
) else (
    echo   全部通过!
    echo ================================================================
    exit /b 0
)
