@echo off
chcp 936 >nul
REM git_tool.bat - Mui Git Automation Tool launcher
REM Calls git_tool.ps1 with all arguments forwarded

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0git_tool.ps1" %*
if %errorlevel% neq 0 pause
