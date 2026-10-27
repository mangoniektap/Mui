@echo off
chcp 936 >nul
title Mui UI Library
cd /d "%~dp0"
cd web

where node >nul 2>nul
if errorlevel 1 goto NONODE

if not exist "node_modules" goto INSTALL
goto START

:INSTALL
echo.
echo [Mui] First run, installing dependencies. Please wait...
call npm install --no-audit --no-fund
if errorlevel 1 goto FAIL

:START
echo.
echo [Mui] Dev server: http://localhost:5173
echo [Mui] Keep this window open. Close it to stop the server.
echo.
call npm run dev -- --open
goto END

:NONODE
echo.
echo [ERROR] Node.js not found in PATH.
echo Install Node.js 22 or newer from https://nodejs.org
echo.
pause
goto END

:FAIL
echo.
echo [ERROR] npm install failed. Check network and try again.
echo.
pause

:END
