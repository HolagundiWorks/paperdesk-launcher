$ErrorActionPreference = 'Stop'
$adb = 'C:\Program Files\platform-tools\adb.exe'
$project = Join-Path $PSScriptRoot '..\android\usb-debug-tile'
& (Join-Path $project 'build.ps1')
& $adb install -r (Join-Path $project 'out\usb-debug-tile.apk')
& $adb shell pm grant local.samsung.usbdebugtile android.permission.WRITE_SECURE_SETTINGS

$tile = 'custom(local.samsung.usbdebugtile/.UsbDebugTileService)'
$current = (& $adb shell settings get secure sysui_qs_tiles).Trim()
if ($current -eq 'null' -or [string]::IsNullOrWhiteSpace($current)) { $current = '' }
$tiles = @($current.Split(',', [System.StringSplitOptions]::RemoveEmptyEntries))
if ($tiles -notcontains $tile) {
    $updated = (@($tile) + $tiles) -join ','
    # adb joins remote shell arguments; quote the value so custom(...) is not
    # parsed by Android's shell as a subshell expression.
    & $adb shell "settings put secure sysui_qs_tiles '$updated'"
}
& $adb shell cmd statusbar collapse
Write-Host 'USB debugging Quick Settings tile installed.'
