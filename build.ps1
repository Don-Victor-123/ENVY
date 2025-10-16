## build.ps1 — compila y ejecuta una prueba rápida con JDK 21
param(
    [string]$JdkBin = "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.8.9-hotspot\\bin",
    [string]$ProjectDir = (Get-Location).Path
)

Write-Host "Usando JDK bin: $JdkBin"
$env:PATH = "$JdkBin;" + $env:PATH

Push-Location $ProjectDir
Write-Host "Compilando con Maven (incluye pruebas unitarias)"
mvn clean package
if ($LASTEXITCODE -ne 0) { Write-Error "Compilación fallida"; Pop-Location; exit 1 }

Write-Host "Compilación OK — lanzando clase Console (salir con Ctrl+C o 0 en el menú)"
java -cp target/classes Console

Pop-Location
