@echo off
set PATH=%SystemRoot%\System32;%SystemRoot%;%PATH%
title PW DHYAN - Stop Web Server
color 0c
echo ========================================================
echo       PW DHYAN • PC Web Browser Server Shutdown
echo ========================================================
echo.
echo Stopping PW DHYAN Server on Port 3300...

:: 1. Terminate process listening on port 3300
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :3300') do (
    echo Terminating PID %%a on Port 3300...
    taskkill /f /pid %%a >nul 2>&1
)

:: 2. Terminate any stray node processes running server.js
wmic process where "name='node.exe' and CommandLine like '%%server.js%%'" call terminate >nul 2>&1

echo.
echo ========================================================
echo [OK] PW DHYAN Web Server has been completely STOPPED!
echo ========================================================
echo.
timeout /t 2 /nobreak >nul
exit
