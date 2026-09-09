[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$ApplicationDirectory,
    [switch]$SmokeTest,
    [switch]$ConcurrentStartupTest,
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

function Wait-ForHealthcheck {
    param(
        [Parameter(Mandatory)]
        [System.Diagnostics.Process]$Process,
        [Parameter(Mandatory)]
        [string]$DataDirectory
    )

    $databasePath = Join-Path $DataDirectory 'hyperion.db'
    $deadline = [DateTime]::UtcNow.AddSeconds(30)
    while (-not (Test-Path -LiteralPath $databasePath) -and -not $Process.HasExited -and [DateTime]::UtcNow -lt $deadline) {
        Start-Sleep -Milliseconds 250
    }
    if (-not (Test-Path -LiteralPath $databasePath)) {
        throw "O teste de inicialização não criou o banco de dados isolado esperado."
    }
    if (-not $Process.WaitForExit(30000)) {
        throw "O processo de inicialização não foi encerrado no prazo esperado."
    }
    if ($Process.ExitCode -ne 0) {
        throw "O processo de inicialização falhou com código $($Process.ExitCode)."
    }
}

if ($SmokeTest -or $ConcurrentStartupTest) {
    $healthDataDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("hyperion-healthcheck-" + [Guid]::NewGuid())
    $launchers = @()
    try {
        $instances = if ($ConcurrentStartupTest) { 2 } else { 1 }
        for ($index = 0; $index -lt $instances; $index++) {
            $launchers += Start-Process -FilePath (Join-Path $applicationPath 'Hyperion.exe') `
                -ArgumentList '--healthcheck', ("--data-dir=" + $healthDataDirectory) -PassThru -WindowStyle Hidden
        }
        foreach ($launcher in $launchers) {
            Wait-ForHealthcheck -Process $launcher -DataDirectory $healthDataDirectory
        }
    } finally {
        foreach ($launcher in $launchers) {
            if (-not $launcher.HasExited) {
                $launcher.Kill()
                $launcher.WaitForExit(5000) | Out-Null
            }
        }
        if (Test-Path -LiteralPath $healthDataDirectory) {
            Remove-Item -LiteralPath $healthDataDirectory -Recurse -Force
        }
    }
}

Write-Output "Pacote Windows válido em: $applicationPath"
