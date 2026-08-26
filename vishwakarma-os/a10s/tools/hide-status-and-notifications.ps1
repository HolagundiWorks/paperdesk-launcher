param([string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }

# Hide only the status bar globally; preserve Samsung's gesture navigation bar.
& $AdbPath shell settings put global policy_control 'immersive.status=*'

# Suppress notification content, badges and heads-up banners.
& $AdbPath shell settings put secure lock_screen_show_notifications 0
& $AdbPath shell settings put secure lock_screen_allow_private_notifications 0
& $AdbPath shell settings put secure lockscreen_show_notifications 0
& $AdbPath shell settings put secure notification_badging 0
& $AdbPath shell settings put global heads_up_notifications_enabled 0
& $AdbPath shell settings put secure heads_up_notifications_enabled 0

# Remove the non-secure lock screen, including Samsung's carrier/flight-mode text.
& $AdbPath shell locksettings set-disabled true

Write-Host 'Status bar and notification surfaces hidden; gesture navigation preserved.'
