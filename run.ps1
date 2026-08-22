$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$candidates = @(
    (Get-Command python -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue),
    'C:\Users\holag\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }
if (-not $candidates) { throw 'Python 3.11+ was not found. Install Python and retry.' }
& $candidates[0] (Join-Path $projectRoot 'src\main.py')
