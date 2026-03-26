@echo off
echo ========================================
echo  Smart Parking System - Build and Run
echo ========================================

if not exist out mkdir out

echo Compiling...
javac -d out ^
  src\smartparking\Vehicle.java ^
  src\smartparking\Car.java ^
  src\smartparking\Bike.java ^
  src\smartparking\ParkingException.java ^
  src\smartparking\ParkingSlot.java ^
  src\smartparking\ParkingManager.java ^
  src\smartparking\QRCodeGenerator.java ^
  src\smartparking\CheckoutServer.java ^
  src\smartparking\MessagingService.java ^
  src\smartparking\QRDialog.java ^
  src\smartparking\QRScannerDialog.java ^
  src\smartparking\CheckoutDialog.java ^
  src\smartparking\ParkingGUI.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed. See errors above.
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo Starting Smart Parking System...
java -cp out smartparking.ParkingGUI
pause
