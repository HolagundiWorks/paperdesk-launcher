$ErrorActionPreference = "Stop"
$env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")

$root = Split-Path -Parent $PSScriptRoot
if ((Split-Path -Leaf $PSScriptRoot) -ne "desktop") {
    $root = $PSScriptRoot
    if (-not (Test-Path (Join-Path $root "PaperDesk.Companion.csproj"))) {
        $root = Split-Path -Parent $PSScriptRoot
    }
}
$desktop = Join-Path $root "desktop"
if (Test-Path (Join-Path $PSScriptRoot "PaperDesk.Companion.csproj")) {
    $desktop = $PSScriptRoot
    $root = Split-Path -Parent $desktop
}

$proj = Join-Path $desktop "PaperDesk.Companion.csproj"
$publish = Join-Path $desktop "bin\Release\net8.0-windows10.0.26100.0\win-x64\publish"
$installer = Join-Path $desktop "installer"
$dist = Join-Path $desktop "dist"

Write-Host "Publishing Vishwakarma companion..."
dotnet publish $proj -c Release -p:Platform=x64 -r win-x64 --self-contained true -o $publish
if ($LASTEXITCODE -ne 0) { throw "Publish failed" }

$exe = Join-Path $publish "Vishwakarma.exe"
if (-not (Test-Path $exe)) { throw "Published exe missing: $exe" }

Write-Host "Building MSI..."
New-Item -ItemType Directory -Force -Path $dist | Out-Null
$wixproj = Join-Path $installer "Vishwakarma.Installer.wixproj"
dotnet build $wixproj -c Release -p:PublishDir="$publish\" -p:AssetsDir="$(Join-Path $desktop 'Assets')\"
if ($LASTEXITCODE -ne 0) { throw "MSI build failed" }

$built = Get-ChildItem -Path (Join-Path $installer "bin") -Recurse -Filter "Vishwakarma.msi" | Select-Object -First 1
if (-not $built) { throw "MSI not found under installer\bin" }
$out = Join-Path $dist "Vishwakarma-1.1.0-x64.msi"
Copy-Item $built.FullName $out -Force
Write-Host "Installer: $out"
Write-Host ("Size: {0:N1} MB" -f ($built.Length / 1MB))
