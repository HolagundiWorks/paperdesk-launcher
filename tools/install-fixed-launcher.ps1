param([string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }

$project = Join-Path $PSScriptRoot '..\android\fixed-launcher'
& (Join-Path $project 'build.ps1')
& $AdbPath install -r (Join-Path $project 'out\fixed-launcher.apk')
& $AdbPath shell cmd package set-home-activity 'local.samsung.fixedlauncher/.MainActivity'
& $AdbPath shell am start -a android.intent.action.MAIN -c android.intent.category.HOME

$resolved = & $AdbPath shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
if ($resolved -notmatch 'local\.samsung\.fixedlauncher') { throw "Fixed launcher did not become HOME: $resolved" }
& $AdbPath shell pm uninstall -k --user 0 app.olauncher

Write-Host 'Fixed 5x3 icon-grid launcher installed as the sole HOME app.'
