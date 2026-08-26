param([string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }

& $AdbPath shell svc wifi disable
& $AdbPath shell svc bluetooth disable
& $AdbPath shell svc data disable
& $AdbPath shell cmd location set-location-enabled --user 0 false
& $AdbPath shell settings put global wifi_scan_always_enabled 0
& $AdbPath shell settings put global ble_scan_always_enabled 0
& $AdbPath shell settings put global wifi_on 0
& $AdbPath shell settings put global bluetooth_on 0
& $AdbPath shell settings put global mobile_data 0
& $AdbPath shell cmd connectivity airplane-mode enable
Write-Host 'Offline mode applied. USB and ADB were not changed.'
