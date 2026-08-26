$ErrorActionPreference = "Stop"
$env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
Set-Location $PSScriptRoot
dotnet build -c Debug -p:Platform=x64
$exe = Join-Path $PSScriptRoot "bin\x64\Debug\net8.0-windows10.0.26100.0\win-x64\Vishwakarma.exe"
if (-not (Test-Path $exe)) { throw "Build output not found: $exe" }
Start-Process $exe -WorkingDirectory (Split-Path $exe)
