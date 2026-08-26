param([string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }

& $AdbPath shell cmd package install-existing --user 0 com.android.wallpaper.livepicker
& $AdbPath shell cmd package install-existing --user 0 com.android.wallpapercropper
& $AdbPath shell cmd appops set app.olauncher WRITE_WALLPAPER default

Write-Host 'Wallpaper selection restored for user 0.'
