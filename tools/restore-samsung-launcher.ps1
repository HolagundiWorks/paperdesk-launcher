param([string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }

& $AdbPath shell cmd package install-existing --user 0 com.sec.android.app.launcher
& $AdbPath shell cmd package set-home-activity 'com.sec.android.app.launcher/.activities.LauncherActivity'
& $AdbPath shell am start -a android.intent.action.MAIN -c android.intent.category.HOME

Write-Host 'Samsung Launcher restored and selected for user 0.'
