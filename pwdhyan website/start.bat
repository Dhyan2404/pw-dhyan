@echo off
set PATH=%SystemRoot%\System32;%SystemRoot%;%PATH%
title PW DHYAN - PC Web Browser Launcher
color 0b
echo ========================================================
echo       PW DHYAN • PC Web Browser Launcher (by Dhyan)
echo ========================================================
echo.

cd /d "%~dp0"

:: Stop any previous instance on port 3300
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :3300') do taskkill /f /pid %%a >nul 2>&1

:: Start Node.js server
echo [1/2] Starting PW Dhyan Web Server...
start "" node server.js

:: Wait for server initialization
timeout /t 2 /nobreak >nul

:: Launch Chrome in dedicated App Mode
echo [2/2] Launching Chrome Web Browser...
set LAUNCHED=0

if exist "%ProgramFiles%\Google\Chrome\Application\chrome.exe" (
    start "" "%ProgramFiles%\Google\Chrome\Application\chrome.exe" --app=http://localhost:3300 --start-maximized
    set LAUNCHED=1
    goto finish
)

if exist "%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe" (
    start "" "%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe" --app=http://localhost:3300 --start-maximized
    set LAUNCHED=1
    goto finish
)

if exist "%LocalAppData%\Google\Chrome\Application\chrome.exe" (
    start "" "%LocalAppData%\Google\Chrome\Application\chrome.exe" --app=http://localhost:3300 --start-maximized
    set LAUNCHED=1
    goto finish
)

:: Edge fallback as App mode
if exist "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" (
    start "" "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" --app=http://localhost:3300 --start-maximized
    set LAUNCHED=1
    goto finish
)

:: Default browser fallback
if %LAUNCHED%==0 (
    start http://localhost:3300
)

:finish
echo.
echo ========================================================
echo PW DHYAN is active at http://localhost:3300
echo Keep this window open while using the browser.
echo ========================================================
echo.
pause
