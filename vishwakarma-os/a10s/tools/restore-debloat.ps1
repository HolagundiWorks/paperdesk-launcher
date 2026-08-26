param([string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
$list = Join-Path $PSScriptRoot 'debloat-packages.txt'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }
$packages = Get-Content -LiteralPath $list | ForEach-Object { $_.Trim() } | Where-Object { $_ -and -not $_.StartsWith('#') } | Select-Object -Unique
foreach ($package in $packages) {
    $result = (& $AdbPath shell cmd package install-existing --user 0 $package 2>&1) -join ' '
    if ($result -match 'installed') { Write-Host "RESTORED $package" } else { Write-Warning "$package`: $result" }
}
Write-Host 'Package restoration complete. Reboot the device.'
