param([string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }

& $AdbPath shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.threebutton
& $AdbPath shell settings put secure navigation_mode 0
& $AdbPath shell settings put global navigation_bar_gesture_while_hidden 0
& $AdbPath shell am broadcast -a com.samsung.systemui.NAVIGATIONBAR_SETTINGS_UPDATE | Out-Null

Write-Host 'Samsung three-button navigation restored.'
