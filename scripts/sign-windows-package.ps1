[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$File,
    [Parameter(Mandatory)]
    [string]$CertificateThumbprint,
    [Parameter(Mandatory)]
    [string]$TimestampServer
)

$ErrorActionPreference = 'Stop'
$packageFile = (Resolve-Path -LiteralPath $File).Path
if (-not $packageFile.EndsWith('.exe', [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Informe um executável .exe para assinatura.'
}

$signTool = (Get-Command 'signtool.exe' -ErrorAction Stop).Source
& $signTool sign /sha1 $CertificateThumbprint /fd SHA256 /tr $TimestampServer /td SHA256 $packageFile
if ($LASTEXITCODE -ne 0) {
    throw "A assinatura falhou (código $LASTEXITCODE)."
}

& $signTool verify /pa /all $packageFile
if ($LASTEXITCODE -ne 0) {
    throw "A verificação da assinatura falhou (código $LASTEXITCODE)."
}

Write-Output "Pacote assinado e validado: $packageFile"
