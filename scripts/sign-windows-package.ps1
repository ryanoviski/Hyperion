[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$InstallerFile,
    [Parameter(Mandatory)]
    [string]$CertificateThumbprint,
    [string]$TimestampServer = 'http://timestamp.digicert.com'
)

$ErrorActionPreference = 'Stop'
$installerPath = (Resolve-Path -LiteralPath $InstallerFile).Path
$signTool = (Get-Command 'signtool.exe' -ErrorAction Stop).Source
$thumbprint = $CertificateThumbprint.Replace(' ', '')

if ($thumbprint -notmatch '^[0-9A-Fa-f]{40}$') {
    throw 'Informe a impressão digital SHA-1 de um certificado de assinatura de código válido.'
}

& $signTool sign /sha1 $thumbprint /fd SHA256 /tr $TimestampServer /td SHA256 /d 'Hyperion' $installerPath
if ($LASTEXITCODE -ne 0) {
    throw "A assinatura do instalador falhou (código $LASTEXITCODE)."
}

& $signTool verify /pa /v $installerPath
if ($LASTEXITCODE -ne 0) {
    throw "A assinatura do instalador não foi validada (código $LASTEXITCODE)."
}

Write-Output "Instalador assinado e validado: $installerPath"
