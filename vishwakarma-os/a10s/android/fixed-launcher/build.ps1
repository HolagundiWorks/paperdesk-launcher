param(
    [string]$Sdk = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$BuildToolsVersion = '35.0.0'
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true
$root = $PSScriptRoot
$tools = Join-Path $Sdk "build-tools\$BuildToolsVersion"
$androidJar = Join-Path $Sdk 'platforms\android-36\android.jar'
$output = Join-Path $root 'out'
$classes = Join-Path $output 'classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null

$sources = Get-ChildItem (Join-Path $root 'src') -Recurse -Filter '*.java' | ForEach-Object FullName
javac -source 8 -target 8 -classpath $androidJar -d $classes $sources
& (Join-Path $tools 'd8.bat') --lib $androidJar --output $output (Get-ChildItem $classes -Recurse -Filter '*.class' | ForEach-Object FullName)
& (Join-Path $tools 'aapt2.exe') compile --dir (Join-Path $root 'res') -o (Join-Path $output 'resources.zip')
& (Join-Path $tools 'aapt2.exe') link --auto-add-overlay -I $androidJar --manifest (Join-Path $root 'AndroidManifest.xml') --min-sdk-version 24 --target-sdk-version 30 -R (Join-Path $output 'resources.zip') -o (Join-Path $output 'unsigned.apk')
Push-Location $output
try { & (Join-Path $tools 'aapt.exe') add 'unsigned.apk' 'classes.dex' } finally { Pop-Location }
& (Join-Path $tools 'zipalign.exe') -f 4 (Join-Path $output 'unsigned.apk') (Join-Path $output 'aligned.apk')

$key = Join-Path $output 'debug.keystore'
if (-not (Test-Path $key)) {
    keytool -genkeypair -keystore $key -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug,O=Samsung Root Manager,C=IN' -keyalg RSA -keysize 2048 -validity 10000
}
& (Join-Path $tools 'apksigner.bat') sign --ks $key --ks-pass pass:android --key-pass pass:android --out (Join-Path $output 'fixed-launcher.apk') (Join-Path $output 'aligned.apk')
Write-Host (Join-Path $output 'fixed-launcher.apk')
