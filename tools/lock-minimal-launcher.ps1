param([string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }

$installed = & $AdbPath shell pm list packages app.olauncher
if ($installed -notmatch 'package:app\.olauncher') {
    throw 'Olauncher must be installed and tested before locking HOME.'
}

& $AdbPath shell cmd package set-home-activity 'app.olauncher/.MainActivity'
& $AdbPath shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
& $AdbPath shell pm uninstall -k --user 0 com.sec.android.app.launcher

Write-Host 'Olauncher is the sole HOME app for user 0. Restore access requires ADB.'
