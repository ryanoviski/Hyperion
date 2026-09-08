[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$ApplicationDirectory
)

$ErrorActionPreference = 'Stop'
$applicationPath = (Resolve-Path -LiteralPath $ApplicationDirectory).Path
$expectedFiles = @(
    (Join-Path $applicationPath 'Hyperion.exe'),
    (Join-Path $applicationPath 'app\Hyperion.cfg'),
    (Join-Path $applicationPath 'runtime\bin\java.dll')
)

$missingFiles = $expectedFiles | Where-Object { -not (Test-Path -LiteralPath $_) }
if ($missingFiles) {
    throw "Pacote inválido. Arquivos ausentes: $($missingFiles -join ', ')"
}

Write-Output "Pacote Windows válido em: $applicationPath"
