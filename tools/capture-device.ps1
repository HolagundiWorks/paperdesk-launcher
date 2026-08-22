param(
    [string]$AdbPath = 'C:\Program Files\platform-tools\adb.exe',
    [string]$OutputDirectory = ''
)
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB not found: $AdbPath" }
$devices = @(& $AdbPath devices | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" })
if ($devices.Count -ne 1) { throw "Exactly one authorized ADB device is required; found $($devices.Count)." }
if (-not $OutputDirectory) {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $OutputDirectory = Join-Path (Split-Path -Parent $PSScriptRoot) "evidence\device-$stamp"
}
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$properties = @(
    'ro.product.model','ro.product.device','ro.product.board','ro.board.platform',
    'ro.build.version.release','ro.build.version.sdk','ro.build.fingerprint',
    'ro.bootloader','ro.boot.sales_code','ro.boot.verifiedbootstate',
    'ro.boot.flash.locked','ro.treble.enabled','ro.vndk.version',
    'ro.build.ab_update','ro.boot.dynamic_partitions','ro.crypto.type','ro.hardware'
)
$record = [ordered]@{ schema = 1; captured_at = (Get-Date).ToUniversalTime().ToString('o'); properties = [ordered]@{} }
foreach ($name in $properties) { $record.properties[$name] = ((& $AdbPath shell getprop $name) -join '').Trim() }
$record.serial_sha256 = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes(((& $AdbPath get-serialno) -join '').Trim()))).ToLowerInvariant()
$record | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $OutputDirectory 'device.json') -Encoding utf8
@(& $AdbPath shell uname -a) | Set-Content -LiteralPath (Join-Path $OutputDirectory 'uname.txt') -Encoding utf8
@(& $AdbPath shell cat /proc/meminfo) | Set-Content -LiteralPath (Join-Path $OutputDirectory 'meminfo.txt') -Encoding utf8
@(& $AdbPath shell ls -l /dev/block/by-name) | Set-Content -LiteralPath (Join-Path $OutputDirectory 'partitions.txt') -Encoding utf8
@(& $AdbPath shell dumpsys usb) | Set-Content -LiteralPath (Join-Path $OutputDirectory 'usb.txt') -Encoding utf8
Get-ChildItem -LiteralPath $OutputDirectory -File | ForEach-Object {
    [pscustomobject]@{ file = $_.Name; size = $_.Length; sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() }
} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $OutputDirectory 'manifest.json') -Encoding utf8
Write-Host "Evidence captured at $OutputDirectory"
