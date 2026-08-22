param([string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }

$project = Join-Path $PSScriptRoot '..\android\black-wallpaper'
& (Join-Path $project 'build.ps1')
$apk = Join-Path $project 'out\black-wallpaper.apk'
& $AdbPath install -r $apk
& $AdbPath shell am start -n 'local.samsung.blackwallpaper/.MainActivity'
Start-Sleep -Seconds 2

& $AdbPath shell cmd appops set app.olauncher WRITE_WALLPAPER ignore
& $AdbPath shell pm uninstall -k --user 0 com.android.wallpaper.livepicker
& $AdbPath shell pm uninstall -k --user 0 com.android.wallpapercropper
& $AdbPath uninstall local.samsung.blackwallpaper

Write-Host 'Pure-black wallpaper applied and UI wallpaper changes blocked.'
