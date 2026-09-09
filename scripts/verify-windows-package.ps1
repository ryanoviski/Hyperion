[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$ApplicationDirectory,
    [switch]$SmokeTest,
    [switch]$RequireSignature
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

if ($RequireSignature) {
    $signature = Get-AuthenticodeSignature -LiteralPath (Join-Path $applicationPath 'Hyperion.exe')
    if ($signature.Status -ne 'Valid') {
        throw "A assinatura do executável é inválida: $($signature.Status)"
    }
}

if ($SmokeTest) {
    $healthDataDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("hyperion-healthcheck-" + [Guid]::NewGuid())
    $launcher = $null
    try {
        $launcher = Start-Process -FilePath (Join-Path $applicationPath 'Hyperion.exe') `
            -ArgumentList '--healthcheck', ("--data-dir=" + $healthDataDirectory) -PassThru -WindowStyle Hidden
        $deadline = [DateTime]::UtcNow.AddSeconds(30)
        while (-not (Test-Path -LiteralPath (Join-Path $healthDataDirectory 'hyperion.db')) -and [DateTime]::UtcNow -lt $deadline) {
            Start-Sleep -Milliseconds 250
        }
        if (-not (Test-Path -LiteralPath (Join-Path $healthDataDirectory 'hyperion.db'))) {
            throw "O teste de inicialização não criou o banco de dados isolado esperado."
        }
    } finally {
        if ($null -ne $launcher -and -not $launcher.HasExited) {
            $launcher.WaitForExit(5000) | Out-Null
        }
        if (Test-Path -LiteralPath $healthDataDirectory) {
            Remove-Item -LiteralPath $healthDataDirectory -Recurse -Force
        }
    }
}

Write-Output "Pacote Windows válido em: $applicationPath"
