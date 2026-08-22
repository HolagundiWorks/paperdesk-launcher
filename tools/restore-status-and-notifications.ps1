param([string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }

& $AdbPath shell settings delete global policy_control
& $AdbPath shell settings put secure lock_screen_show_notifications 1
& $AdbPath shell settings put secure lock_screen_allow_private_notifications 1
& $AdbPath shell settings delete secure lockscreen_show_notifications
& $AdbPath shell settings put secure notification_badging 1
& $AdbPath shell settings delete global heads_up_notifications_enabled
& $AdbPath shell settings delete secure heads_up_notifications_enabled
& $AdbPath shell locksettings set-disabled false

Write-Host 'Status bar, lock screen and standard notification surfaces restored.'
