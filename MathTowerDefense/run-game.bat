@echo off
title Math Tower Defense 2D
cd /d "%~dp0"

echo ===================================================
echo Starting Math Tower Defense 2D...
echo ===================================================
echo.

java -cp "bin;libs/*;resources" com.mathtowerdefense.gui.MainGUI
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Game exited with error code %ERRORLEVEL%.
    pause
)
