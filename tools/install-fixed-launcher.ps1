param(
    [string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe',
    [string]$ArcticonsApk = 'D:\Samsung-A10s-Recovery\apps\icons\Arcticons-15.0.5-normal-release.apk'
)
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }

$project = Join-Path $PSScriptRoot '..\android\fixed-launcher'
if ((& $AdbPath shell pm list packages com.donnnno.arcticons) -notmatch 'com\.donnnno\.arcticons') {
    if (-not (Test-Path -LiteralPath $ArcticonsApk)) { throw "Arcticons APK not found: $ArcticonsApk" }
    & $AdbPath install -r $ArcticonsApk
}
& (Join-Path $project 'build.ps1')
& $AdbPath install -r (Join-Path $project 'out\fixed-launcher.apk')
& $AdbPath shell settings put system double_tap_to_sleep 0
# One UI hosts its privileged gesture-recents/Quickstep service in Samsung
# Launcher. Keep that package installed but leave our fixed launcher as HOME.
& $AdbPath shell cmd package install-existing --user 0 com.sec.android.app.launcher
& $AdbPath shell cmd package set-home-activity 'local.samsung.fixedlauncher/.MainActivity'
& $AdbPath shell am start -a android.intent.action.MAIN -c android.intent.category.HOME

$resolved = & $AdbPath shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
if ($resolved -notmatch 'local\.samsung\.fixedlauncher') { throw "Fixed launcher did not become HOME: $resolved" }
& $AdbPath shell pm uninstall -k --user 0 app.olauncher

Write-Host 'Fixed 5x3 icon-grid launcher installed as the sole HOME app.'
