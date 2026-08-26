param([string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }

& $AdbPath shell cmd connectivity airplane-mode disable
& $AdbPath shell settings put global wifi_scan_always_enabled 1
& $AdbPath shell settings put global ble_scan_always_enabled 1
& $AdbPath shell cmd location set-location-enabled --user 0 true
& $AdbPath shell svc data enable
& $AdbPath shell svc bluetooth enable
& $AdbPath shell svc wifi enable
Write-Host 'Connectivity restored.'
