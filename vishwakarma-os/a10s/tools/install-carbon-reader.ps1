param(
    [string]$Source = 'D:\Samsung-A10s-Recovery\source\BookStory'
)
$ErrorActionPreference = 'Stop'
$adb = 'C:\Program Files\platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $Source)) { throw "Carbon Reader source not found: $Source" }

$previousGradleHome = $env:GRADLE_USER_HOME
try {
    $env:GRADLE_USER_HOME = 'D:\Samsung-A10s-Recovery\gradle-home'
    & (Join-Path $Source 'gradlew.bat') -p $Source ':app:assembleRelease'
} finally {
    $env:GRADLE_USER_HOME = $previousGradleHome
}

$apk = Join-Path $Source 'app\build\outputs\apk\release\app-release.apk'
& $adb install -r $apk
& (Join-Path $PSScriptRoot '..\android\fixed-launcher\build.ps1')
& $adb install -r (Join-Path $PSScriptRoot '..\android\fixed-launcher\out\fixed-launcher.apk')
Write-Host 'Carbon Reader and the updated dashboard are installed.'
