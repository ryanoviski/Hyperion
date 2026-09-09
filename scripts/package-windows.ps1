[CmdletBinding()]
param(
    [ValidateSet('app-image', 'exe')]
    [string]$Type = 'exe',
    [string]$SigningCertificateThumbprint,
    [string]$TimestampServer = 'http://timestamp.digicert.com'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$pomPath = Join-Path $projectRoot 'pom.xml'
[xml]$pom = Get-Content -LiteralPath $pomPath
$version = $pom.project.version
$maven = (Get-Command 'mvn.cmd' -ErrorAction Stop).Source
$jpackage = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
} else {
    (Get-Command 'jpackage.exe' -ErrorAction Stop).Source
}

if (-not (Test-Path -LiteralPath $jpackage)) {
    throw "jpackage não foi encontrado. Use um JDK 21 para empacotar o Hyperion."
}

Push-Location $projectRoot
try {
    & $maven clean verify
    if ($LASTEXITCODE -ne 0) {
        throw "A validação Maven falhou (código $LASTEXITCODE)."
    }

    $targetDirectory = Join-Path $projectRoot 'target'
    $inputDirectory = Join-Path $targetDirectory 'package-input'
    $jarName = "hyperion-$version.jar"
    $jarPath = Join-Path $targetDirectory $jarName
    $outputDirectory = Join-Path $targetDirectory 'installer'
    $iconPath = Join-Path $projectRoot 'src\main\resources\images\app-icon.ico'

    if (-not (Test-Path -LiteralPath $jarPath)) {
        throw "O JAR esperado não foi gerado: $jarPath"
    }
    if (-not (Test-Path -LiteralPath (Join-Path $inputDirectory 'lib'))) {
        throw "As dependências de execução não foram preparadas em $inputDirectory"
    }

    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $inputDirectory $jarName) -Force
    if (Test-Path -LiteralPath $outputDirectory) {
        Remove-Item -LiteralPath $outputDirectory -Recurse -Force
    }
    New-Item -ItemType Directory -Path $outputDirectory | Out-Null

    $arguments = @(
        '--type', $Type,
        '--dest', $outputDirectory,
        '--name', 'Hyperion',
        '--app-version', $version,
        '--vendor', 'Hyperion',
        '--description', 'Sistema de gestão Hyperion',
        '--input', $inputDirectory,
        '--main-jar', $jarName,
        '--main-class', 'com.hyperion.app.AppLauncher',
        '--icon', $iconPath
    )

    if ($Type -eq 'exe') {
        $arguments += @(
            '--win-shortcut', '--win-menu', '--win-menu-group', 'Hyperion', '--win-dir-chooser', '--win-per-user-install',
            '--win-upgrade-uuid', 'e6507986-59db-4ca4-94d4-a75422128338'
        )
    }

    & $jpackage @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "O jpackage falhou (código $LASTEXITCODE)."
    }

    if (-not [string]::IsNullOrWhiteSpace($SigningCertificateThumbprint)) {
        if ($Type -ne 'exe') {
            throw 'A assinatura só é suportada ao gerar o instalador EXE.'
        }
        $installerFile = Join-Path $outputDirectory ("Hyperion-" + $version + '.exe')
        & (Join-Path $PSScriptRoot 'sign-windows-package.ps1') `
            -InstallerFile $installerFile `
            -CertificateThumbprint $SigningCertificateThumbprint `
            -TimestampServer $TimestampServer
    }
} finally {
    Pop-Location
}
