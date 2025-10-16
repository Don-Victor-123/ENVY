@echo off
javac -encoding UTF-8 Console.java PruebaImpresion.java CorteManager.java
if errorlevel 1 (
  echo Error al compilar.
  pause
  exit /b 1
)
echo.
java Console
echo.
