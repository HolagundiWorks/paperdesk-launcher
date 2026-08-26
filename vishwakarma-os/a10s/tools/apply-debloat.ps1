param([string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
$list = Join-Path $PSScriptRoot 'debloat-packages.txt'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
& $AdbPath get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'An authorized ADB device is required.' }
$packages = Get-Content -LiteralPath $list | ForEach-Object { $_.Trim() } | Where-Object { $_ -and -not $_.StartsWith('#') } | Select-Object -Unique
$installed = @(& $AdbPath shell pm list packages --user 0) -replace '^package:', ''
$removed = 0
foreach ($package in $packages) {
    if ($installed -notcontains $package) { Write-Host "SKIP $package"; continue }
    $result = (& $AdbPath shell pm uninstall --user 0 $package 2>&1) -join ' '
    if ($result -match 'Success') { $removed++; Write-Host "REMOVED $package" } else { Write-Warning "$package`: $result" }
}
Write-Host "Removed $removed packages for user 0. System partitions were not modified."
