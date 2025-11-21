@echo off
REM BridgeOne Firmware Flash Script for COM8 (YD-ESP32-S3)
REM
REM Usage:
REM   1. ESP-IDF 환경을 먼저 활성화: C:\Espressif\frameworks\esp-idf-v5.5\export.bat
REM   2. 이 스크립트 실행

echo ============================================
echo BridgeOne Firmware Flash to COM8
echo ============================================
echo.

cd /d "F:\C\Programming\MobileDevelopment\Projects\Android\BridgeOne\src\board\BridgeOne"

echo Step 1: Checking idf.py availability...
where idf.py >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] idf.py not found. Please activate ESP-IDF environment first:
    echo.
    echo     C:\Espressif\frameworks\esp-idf-v5.5\export.bat
    echo.
    pause
    exit /b 1
)

echo Step 2: Checking build directory...
if not exist "build\BridgeOne.bin" (
    echo [ERROR] Firmware build not found. Building now...
    idf.py build
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Build failed!
        pause
        exit /b 1
    )
)

echo Step 3: Flashing to COM8...
idf.py -p COM8 flash
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Flash failed! Check:
    echo   - Is COM8 the correct port?
    echo   - Is the board connected?
    echo   - Is another program using COM8?
    pause
    exit /b 1
)

echo.
echo ============================================
echo Flash completed successfully!
echo ============================================
echo.
echo Now checking if HID device is recognized...
timeout /t 3 /nobreak >nul

powershell -ExecutionPolicy Bypass -File "..\..\..\scripts\check-hid-devices.ps1"

echo.
echo Press any key to start monitor (Ctrl+] to exit)...
pause >nul

idf.py -p COM8 monitor
