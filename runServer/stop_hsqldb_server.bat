@echo off
echo Stopping HSQLDB Server...

echo Finding and stopping HSQLDB processes...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :9011') do (
    taskkill /PID %%a /F >nul 2>&1
)

echo Checking if port 9011 is still in use...
netstat -aon ^| findstr :9011 >nul 2>&1
if %errorlevel% equ 0 (
    echo Warning: Port 9011 is still in use
) else (
    echo Port 9011 is now free
)

echo HSQLDB server stopped successfully!
pause