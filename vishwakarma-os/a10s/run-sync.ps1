$ErrorActionPreference = "Stop"
$python = Get-Command python -ErrorAction SilentlyContinue
if ($python -and -not ($python.Source -like '*WindowsApps*')) {
    & $python.Source "$PSScriptRoot\src\sync_main.py"
    exit $LASTEXITCODE
}

$bundledPython = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
if (Test-Path -LiteralPath $bundledPython) {
    & $bundledPython "$PSScriptRoot\src\sync_main.py"
    exit $LASTEXITCODE
}

throw 'Python 3 with Tk support is required to run Carbon Device Sync.'
