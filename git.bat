@echo off
chcp 936 >nul
REM git.bat - Mui Git Automation Tool launcher
REM Calls git.ps1 with all arguments forwarded

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0git.ps1" %*
if %errorlevel% neq 0 pause
