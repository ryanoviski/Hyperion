[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$outputDirectory = Join-Path $projectRoot 'target\security-scan'
$reportPath = Join-Path $outputDirectory 'osv-scanner.json'
$scanner = Get-Command 'osv-scanner.exe' -ErrorAction SilentlyContinue

if ($null -eq $scanner) {
    $wingetPackages = Join-Path $env:LOCALAPPDATA 'Microsoft\WinGet\Packages'
    $scannerPath = Get-ChildItem -Path $wingetPackages -Filter 'osv-scanner.exe' -File -Recurse -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
    if ($null -ne $scannerPath) {
        $scanner = Get-Command $scannerPath -ErrorAction Stop
    }
}
if ($null -eq $scanner) {
    throw 'OSV-Scanner não foi encontrado. Instale Google.OSVScanner antes de executar a varredura SCA.'
}

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
& $scanner.Source scan source $projectRoot --include-git-root --recursive --format json --output-file $reportPath
if ($LASTEXITCODE -ne 0) {
    throw "A varredura SCA encontrou vulnerabilidades ou falhou (código $LASTEXITCODE). Consulte $reportPath."
}

Write-Output "Varredura SCA aprovada: $reportPath"
