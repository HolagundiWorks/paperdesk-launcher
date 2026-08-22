param([string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }

# Retain useful offline controls and remove every connectivity/account tile.
& $AdbPath shell settings put secure sysui_qs_tiles 'SoundMode,RotationLock,Flashlight,BlueLightFilter,Dnd'
& $AdbPath shell settings put secure icon_blacklist 'rotate,headset,wifi,ethernet,mobile,airplane,bluetooth,hotspot,cast'

# Samsung blocks component-level changes to Settings from the shell. Remove the
# package only for user 0; its APK remains in /system and is recoverable over ADB.
& $AdbPath shell pm uninstall -k --user 0 com.android.settings

& $AdbPath shell svc wifi disable
& $AdbPath shell svc bluetooth disable
& $AdbPath shell svc data disable
& $AdbPath shell settings put global wifi_on 0
& $AdbPath shell settings put global bluetooth_on 0
& $AdbPath shell settings put global mobile_data 0
& $AdbPath shell cmd location set-location-enabled --user 0 false
& $AdbPath shell cmd connectivity airplane-mode enable

Write-Host 'Appliance UI applied. ADB remains the recovery path.'
