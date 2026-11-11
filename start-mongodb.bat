@echo off
title MongoDB Server - Math Tower Defense
echo ===================================================
echo Starting MongoDB Server for Math Tower Defense 2D
echo Connection URL: mongodb://localhost:27017
echo Database Name:  mathtowerdefense
echo ===================================================
echo.

if not exist "%~dp0..\mongodb_data" mkdir "%~dp0..\mongodb_data"

"C:\Program Files\MongoDB\Server\8.3\bin\mongod.exe" --dbpath "%~dp0..\mongodb_data" --port 27017 --bind_ip 127.0.0.1
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [NOTE] If MongoDB is already running as a Windows service, you can connect directly in MongoDB Compass!
    pause
)
