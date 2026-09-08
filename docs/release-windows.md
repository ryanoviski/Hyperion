# Entrega para Windows

## Versão e armazenamento local

A versão do aplicativo é definida em `pom.xml`. O pacote Windows recebe a mesma versão e a mostra no título da janela.

Em Windows, os dados locais ficam em `%APPDATA%\Hyperion`:

- `hyperion.db`: banco SQLite;
- `backups\`: pacotes `.zip` com banco e anexos criados pelo aplicativo;
- `attachments\`: anexos financeiros.

Uma instalação antiga com `data\hyperion.db` é copiada automaticamente para esse local apenas na primeira abertura, quando ainda não há banco em `%APPDATA%\Hyperion`.

## Gerar o instalador

Pré-requisitos para a máquina de entrega: JDK 21, Maven 3.9+ e WiX Toolset (necessário pelo `jpackage` para criar `.exe` no Windows).

```powershell
.\scripts\package-windows.ps1
```

O comando executa `mvn clean verify`, prepara as dependências de execução e gera o instalador em `target\installer`. O instalador configura ícone, atalho na Área de Trabalho e entrada no Menu Iniciar. Para apenas inspecionar uma imagem portátil sem WiX:

```powershell
.\scripts\package-windows.ps1 -Type app-image
.\scripts\verify-windows-package.ps1 -ApplicationDirectory .\target\installer\Hyperion -SmokeTest
```

O smoke test inicializa o aplicativo empacotado em uma pasta de dados temporária, aplica as migrações e confirma que o banco é criado sem IDE ou Maven em execução. A CI executa esse mesmo fluxo para a imagem portátil.

## Assinatura do instalador

Antes da distribuição externa, assine o instalador com um certificado de assinatura de código válido e um servidor de carimbo do tempo:

```powershell
.\scripts\sign-windows-package.ps1 `
  -File .\target\installer\Hyperion-1.0.0.exe `
  -CertificateThumbprint SEU_THUMBPRINT `
  -TimestampServer https://timestamp.seu-fornecedor.com
```

O certificado e o servidor de carimbo são insumos externos e não devem ser armazenados no repositório. Valide a assinatura antes da publicação.

## Validação em máquina limpa

Antes de liberar uma versão, valide o `.exe` em uma VM ou computador Windows sem JDK, Maven, IDE ou checkout do projeto:

1. Instale o `.exe` e confirme o ícone, o atalho e a entrada no Menu Iniciar.
2. Abra pelo atalho; o assistente de primeiro uso deve aparecer sem terminal ou dependência de desenvolvimento.
3. Cadastre cliente e produto, faça uma venda com desconto e uma venda em crediário; registre uma parcela paga.
4. Inclua um anexo e confira que foi salvo em `%APPDATA%\Hyperion\attachments`.
5. Crie e restaure um backup `.zip`; confirme os dados e os comprovantes esperados.
6. Feche e reabra o aplicativo. Confirme que os dados persistem em `%APPDATA%\Hyperion`.
7. Exporte CSV, Excel e PDF com nomes acentuados, ponto e vírgula e quebra de linha; confira os resultados no Excel e em um leitor de PDF.
