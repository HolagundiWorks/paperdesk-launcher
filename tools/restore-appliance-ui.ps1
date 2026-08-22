param([string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }

& $AdbPath shell cmd package install-existing --user 0 com.android.settings
& $AdbPath shell settings put secure sysui_qs_tiles 'Wifi,SoundMode,Bluetooth,RotationLock,AirplaneMode,Flashlight,MobileData,BlueLightFilter,Hotspot,Location,Dnd'
& $AdbPath shell settings put secure icon_blacklist 'rotate,headset'

Write-Host 'Settings UI and standard quick controls restored. Radios remain off until enabled explicitly.'
