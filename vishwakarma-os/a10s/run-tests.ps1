$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$python = 'C:\Users\holag\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
if (-not (Test-Path -LiteralPath $python)) { $python = (Get-Command python -ErrorAction Stop).Source }
$env:PYTHONPATH = Join-Path $projectRoot 'src'
& $python -m unittest discover -s (Join-Path $projectRoot 'tests') -v
