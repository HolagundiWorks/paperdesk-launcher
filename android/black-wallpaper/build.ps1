param(
    [string]$Sdk = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$BuildToolsVersion = '35.0.0'
)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$tools = Join-Path $Sdk "build-tools\$BuildToolsVersion"
$androidJar = Join-Path $Sdk 'platforms\android-36\android.jar'
$output = Join-Path $root 'out'
$classes = Join-Path $output 'classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null

javac -source 8 -target 8 -classpath $androidJar -d $classes (Join-Path $root 'src\local\samsung\blackwallpaper\MainActivity.java')
& (Join-Path $tools 'd8.bat') --lib $androidJar --output $output (Join-Path $classes 'local\samsung\blackwallpaper\MainActivity.class')
& (Join-Path $tools 'aapt2.exe') link -I $androidJar --manifest (Join-Path $root 'AndroidManifest.xml') --min-sdk-version 24 --target-sdk-version 30 -o (Join-Path $output 'unsigned.apk')
Push-Location $output
try {
    & (Join-Path $tools 'aapt.exe') add 'unsigned.apk' 'classes.dex'
} finally {
    Pop-Location
}
& (Join-Path $tools 'zipalign.exe') -f 4 (Join-Path $output 'unsigned.apk') (Join-Path $output 'aligned.apk')

$key = Join-Path $output 'debug.keystore'
if (-not (Test-Path $key)) {
    keytool -genkeypair -keystore $key -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug,O=Samsung Root Manager,C=IN' -keyalg RSA -keysize 2048 -validity 10000
}
& (Join-Path $tools 'apksigner.bat') sign --ks $key --ks-pass pass:android --key-pass pass:android --out (Join-Path $output 'black-wallpaper.apk') (Join-Path $output 'aligned.apk')
Write-Host (Join-Path $output 'black-wallpaper.apk')
