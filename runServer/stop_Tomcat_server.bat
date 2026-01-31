@echo off
echo Stopping Tomcat Server...

echo Finding and stopping Tomcat processes...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :9001') do (
    taskkill /PID %%a /F >nul 2>&1
)

echo Checking if port 9001 is still in use...
netstat -aon ^| findstr :9001 >nul 2>&1
if %errorlevel% equ 0 (
    echo Warning: Port 9001 is still in use
) else (
    echo Port 9001 is now free
)

echo Tomcat server stopped successfully!
pause