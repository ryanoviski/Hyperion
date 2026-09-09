# Hyperion

**Sistema desktop de gestão para pequenos negócios.**

O Hyperion reúne cadastro de clientes e produtos, controle de estoque, vendas, crediário, financeiro, relatórios e backups em uma aplicação local para Windows. Os dados permanecem no computador utilizado, sem necessidade de instalar ou administrar um servidor de banco de dados.

Versão atual: **1.0.0** · Licença: [MIT](LICENSE)

## Sobre o projeto

O Hyperion foi desenvolvido para centralizar operações que, em negócios pequenos, muitas vezes ficam espalhadas entre cadernos, planilhas e anotações. O sistema permite cadastrar os itens comercializados, registrar vendas e pagamentos, acompanhar estoque e consultar resultados em relatórios.

O funcionamento é local (*local-first*): o banco SQLite, os anexos financeiros, os backups e os logs ficam no perfil do usuário do Windows. A aplicação pode ser usada offline depois de instalada.

## Funcionalidades

### Dashboard

- Mostra vendas do dia, quantidade de clientes e produtos ativos e o saldo atual.
- Exibe as últimas vendas registradas.
- Oferece atalhos para cadastrar cliente, cadastrar produto, consultar estoque e iniciar venda.
- Gera alertas para parcelas vencidas ou com vencimento no dia e para produtos no estoque mínimo.

### Clientes

- Cadastro e edição de nome, documento, telefone, e-mail, endereço e observações.
- Pesquisa por nome, CPF/CNPJ, telefone ou e-mail.
- Filtro por status e ativação/desativação de cadastros.

### Produtos

- Cadastro de nome, descrição, preço, custo, estoque mínimo, categoria, código de barras e fornecedor.
- Pesquisa por nome, categoria, código ou fornecedor.
- Controle de status ativo/inativo.
- Alerta de estoque mínimo no dashboard.

### Estoque

- Registro manual de entradas e saídas com quantidade e motivo obrigatório.
- Histórico recente de movimentações.
- Atualização automática do estoque ao concluir ou cancelar uma venda.
- Bloqueio de saídas e vendas sem saldo disponível.

### Vendas

- Seleção de cliente e produtos para montagem de carrinho.
- Definição de quantidade, desconto em reais e forma de pagamento.
- Formas disponíveis: Dinheiro, PIX, Cartão de crédito, Cartão de débito e Crediário.
- Geração de parcelas para vendas em crediário, de 1 a 10 parcelas, com data do primeiro vencimento.
- Consulta das vendas recentes e cancelamento com motivo. O cancelamento devolve o estoque e cancela parcelas ainda abertas; não é permitido quando já existe parcela paga.

### Crediário

- Acompanhamento de parcelas abertas, vencidas e pagas.
- Pesquisa por cliente e filtro por status.
- Registro de recebimento com responsável, forma de pagamento e observação.
- Consulta do saldo em aberto por cliente e histórico de pagamentos.

### Financeiro

- Indicadores de entradas realizadas, despesas, saldo atual e lucro do mês.
- Registro e remoção de despesas por período.
- Anexo de comprovantes financeiros em PDF, PNG ou JPG/JPEG, de até 10 MB.
- Visualização e exclusão de anexos vinculados a despesas.

### Relatórios

- Filtros por período, intervalo personalizado, cliente, forma de pagamento, categoria e fornecedor.
- Indicadores de total vendido, quantidade de vendas e ticket médio.
- Resumo por forma de pagamento e lista dos dez produtos mais vendidos.
- Exportação do relatório visível para CSV, Excel (`.xlsx`) e PDF.

### Configurações

- Configuração, alteração e remoção de PIN.
- Temas claro e escuro, com preferência persistida localmente.
- Criação e restauração de backups.

## Como funciona

```text
Produto cadastrado
       ↓
Entrada de estoque
       ↓
Venda e baixa do estoque
       ↓
Pagamento imediato ───────→ Financeiro e relatórios
       │
       └── Crediário → Parcelas → Recebimentos → Financeiro
```

Vendas concluídas compõem os relatórios. Para o saldo atual, vendas com pagamento imediato são consideradas como entrada; em vendas no crediário, o valor entra quando a parcela é marcada como paga. O lucro mensal considera vendas, custos registrados nos itens vendidos e despesas do mês.

## Instalação

### Para usuários

O pacote distribuído atualmente é um instalador **`.exe` para Windows**. Ele inclui o runtime necessário; não é preciso instalar Java, Maven ou SQLite separadamente.

1. Acesse a página de [Releases](https://github.com/ryanoviski/Hyperion/releases).
2. Baixe o instalador da versão desejada, por exemplo `Hyperion-1.0.0.exe`.
3. Execute o arquivo baixado e siga as etapas do instalador.
4. Ao terminar, inicie o Hyperion pelo atalho criado pelo Windows ou pelo Menu Iniciar.

> Antes de desinstalar o aplicativo ou trocar de computador, crie um backup pela tela **Configurações**. Os dados ficam separados dos arquivos de instalação.

### Primeira execução

Na primeira abertura, o Hyperion cria o banco local e exibe a tela de configuração do PIN. É possível criar um PIN nesse momento ou escolher **Pular por agora**.

Após essa etapa, o sistema abre no dashboard. Para começar a operar, cadastre clientes e produtos, registre a entrada inicial do estoque e então monte as vendas.

## Uso básico

1. Em **Clientes**, cadastre quem realizará as compras.
2. Em **Produtos**, informe preço, custo e estoque mínimo.
3. Em **Estoque**, registre a entrada dos itens disponíveis.
4. Em **Vendas**, selecione o cliente, adicione produtos ao carrinho, escolha a forma de pagamento e finalize.
5. Para vendas em crediário, informe a quantidade de parcelas e o primeiro vencimento. Depois, registre os recebimentos em **Crediário**.
6. Em **Financeiro**, registre despesas e, quando necessário, anexe seus comprovantes.
7. Use **Relatórios** para filtrar os resultados e exportá-los.

## Armazenamento de dados

No Windows, o Hyperion usa o diretório `%APPDATA%\Hyperion`:

| Caminho | Conteúdo |
| --- | --- |
| `hyperion.db` | Banco de dados SQLite da aplicação. |
| `backups\` | Pacotes de backup `.zip`. |
| `attachments\` | Comprovantes anexados às despesas financeiras. |
| `logs\` | Logs rotativos para diagnóstico. |

O SQLite é embutido na aplicação, portanto não há serviço de banco de dados para instalar. Instalações antigas que ainda possuem `data\hyperion.db` podem ser migradas automaticamente na primeira abertura, quando ainda não existir um banco no diretório atual.

## Backup e restauração

Em **Configurações**:

1. Clique em **Criar backup agora** para gerar uma cópia consistente.
2. O Hyperion cria um arquivo `.zip` em `%APPDATA%\Hyperion\backups`.
3. O pacote inclui o banco SQLite e os anexos financeiros. Os 30 backups mais recentes são mantidos automaticamente.
4. Para restaurar, clique em **Restaurar backup**, selecione um `.zip` do Hyperion e confirme a substituição dos dados atuais.
5. Um backup de segurança dos dados atuais é criado antes da restauração. Reinicie o aplicativo após concluir o processo.

Backups legados em `.db` também podem ser selecionados, mas eles contêm somente o banco de dados; anexos não fazem parte desse formato. O sistema valida integridade, estrutura e referências do banco antes de restaurá-lo.

## Segurança e aparência

O PIN é opcional e protege o acesso ao aplicativo neste computador. A troca ou remoção do PIN exige informar o PIN atual. O valor não é salvo em texto puro, e tentativas de desbloqueio incorretas recebem uma espera progressiva. Quando o PIN está ativo, a sessão é bloqueada automaticamente após 10 minutos sem interação.

Em **Configurações → Aparência**, escolha entre os temas **Escuro** e **Claro**. A opção selecionada é mantida para as próximas aberturas do aplicativo.

## Tecnologias utilizadas

| Tecnologia | Uso no projeto |
| --- | --- |
| Java 21 | Linguagem e runtime de desenvolvimento. |
| JavaFX 21.0.4 | Interface desktop, FXML e estilos CSS. |
| Maven | Build, dependências, testes e execução de desenvolvimento. |
| SQLite JDBC 3.46.1.0 | Persistência local em SQLite. |
| Apache PDFBox 3.0.3 | Geração de relatórios PDF e visualização de comprovantes PDF. |
| JUnit Jupiter 5.10.3 | Testes automatizados. |
| `jpackage` e WiX Toolset | Geração do instalador Windows. |

## Desenvolvimento

### Requisitos

- JDK 21
- Maven 3.9 ou superior para o processo de empacotamento Windows
- IDE opcional, como IntelliJ IDEA
- WiX Toolset para gerar o instalador `.exe` no Windows

### Executando em ambiente de desenvolvimento

```bash
git clone https://github.com/ryanoviski/Hyperion.git
cd Hyperion
mvn javafx:run
```

O primeiro comando do Maven baixa as dependências necessárias. Ao executar pela IDE, importe o projeto como Maven, selecione o JDK 21 e inicie `com.hyperion.app.AppLauncher` ou use a meta `javafx:run` do Maven.

### Testes

Execute a suíte padrão com:

```bash
mvn test
```

Ela cobre migrações do banco, segurança do PIN, anexos, backup e restauração, crediário, financeiro, exportações, filtros de relatórios, vendas e estoque.

Para incluir as verificações de layout das telas e carga de relatórios, execute:

```bash
mvn -Prelease-validation verify
```

O perfil adicional valida as telas principais em resoluções de 1366×768 e 1920×1080 e executa um cenário de carga para relatórios.

### Qualidade e segurança

O workflow de integração contínua para Windows executa a varredura SCA com OSV-Scanner, gera uma imagem portátil do aplicativo e roda um *smoke test* do pacote. Localmente, a varredura pode ser executada após instalar o OSV-Scanner:

```powershell
.\scripts\run-security-scan.ps1
```

## Build e distribuição

### Build Maven

```bash
mvn clean verify
```

O JAR principal é gerado em `target\hyperion-1.0.0.jar`. Durante o empacotamento Maven, as dependências de execução são preparadas em `target\package-input\lib`.

### Gerando o instalador Windows

Com JDK 21, Maven e WiX Toolset disponíveis:

```powershell
.\scripts\package-windows.ps1
```

O script executa `mvn clean verify`, prepara as dependências e gera o instalador em `target\installer\Hyperion-1.0.0.exe`. O pacote configura ícone, atalho na Área de Trabalho, entrada no Menu Iniciar, seleção de diretório e instalação por usuário.

Para gerar e inspecionar apenas uma imagem portátil, sem criar instalador:

```powershell
.\scripts\package-windows.ps1 -Type app-image
.\scripts\verify-windows-package.ps1 -ApplicationDirectory .\target\installer\Hyperion -SmokeTest -ConcurrentStartupTest
```

O teste acima inicializa uma ou duas instâncias do pacote usando dados temporários e confirma a criação do banco sem IDE ou Maven em execução.

O repositório também possui um fluxo opcional de assinatura de código para o instalador. Ele requer certificado e ferramentas externos, que não devem ser armazenados no projeto.

## Estrutura do projeto

```text
Hyperion/
├── .github/workflows/       # Integração contínua
├── docs/                    # Documentação e imagens futuras
├── scripts/                 # Empacotamento, validação e assinatura Windows
├── src/
│   ├── main/
│   │   ├── java/com/hyperion/
│   │   │   ├── app/         # Inicialização da aplicação
│   │   │   ├── config/      # SQLite e migrations
│   │   │   ├── controller/  # Controladores JavaFX
│   │   │   ├── model/       # Entidades e objetos de relatório
│   │   │   ├── repository/  # Consultas e persistência
│   │   │   ├── service/     # Regras de negócio
│   │   │   └── util/        # Tema, sessão, formatação e suporte
│   │   └── resources/
│   │       ├── css/         # Temas claro e escuro
│   │       ├── fxml/        # Telas JavaFX
│   │       └── images/      # Ícones da aplicação
│   └── test/java/           # Testes automatizados
├── pom.xml                  # Build Maven e dependências
└── LICENSE                  # Licença MIT
```

## Arquitetura

O código é organizado em camadas de apresentação, negócio e persistência:

```text
JavaFX (FXML e CSS)
        ↓
Controllers
        ↓
Services
        ↓
Repositories
        ↓
SQLite
```

Os controladores coordenam a interação das telas. Os serviços concentram validações e regras, como disponibilidade de estoque, parcelas e cálculo financeiro. Os repositórios executam o acesso ao SQLite. `config` inicializa o banco e aplica migrations, enquanto `util` oferece recursos transversais como tema, sessão, PIN, tarefas assíncronas e logs.

## Banco de dados

O Hyperion utiliza SQLite com migrations versionadas e transacionais. As principais entidades persistidas são:

- `customers` e `products`
- `stock_movements`
- `sales` e `sale_items`
- `credit_installments` e `credit_payments`
- `expenses` e `attachments`
- `app_settings`, `pin_attempts` e `schema_migrations`

As relações entre clientes, produtos, vendas, itens, parcelas e pagamentos usam chaves estrangeiras. Valores monetários são armazenados em centavos inteiros para preservar precisão, e o banco é aberto com chaves estrangeiras ativas, WAL e tempo de espera para concorrência.

## Cancelamentos

Ao cancelar uma venda concluída, o sistema exige um motivo, marca a venda como cancelada, devolve os itens ao estoque e registra as movimentações de estorno. Em vendas no crediário, parcelas abertas são canceladas junto com a venda. Vendas com alguma parcela já paga não podem ser canceladas.

Vendas canceladas não entram nos indicadores e relatórios de vendas concluídas.

## Solução de problemas

### O aplicativo não inicia

1. Feche qualquer instância aberta do Hyperion.
2. Verifique os arquivos em `%APPDATA%\Hyperion\logs`.
3. Preserve a pasta de dados e crie ou copie um backup antes de qualquer tentativa de restauração.
4. Se o problema persistir, reinstale o aplicativo sem apagar os dados locais e consulte os logs gerados.

### Não consigo restaurar um backup

- Confirme que o arquivo é um backup `.zip` do Hyperion ou um backup legado `.db`.
- Não feche o aplicativo durante a restauração.
- Lembre-se de que backups `.db` legados não incluem anexos.
- O sistema preserva um backup antes da restauração; use-o caso precise retornar ao estado anterior.

### O banco de dados não pode ser acessado

Verifique se o usuário do Windows tem permissão de leitura e gravação em `%APPDATA%\Hyperion` e se há espaço livre em disco. Não apague `hyperion.db` como primeira medida; faça backup ou copie a pasta de dados antes de qualquer intervenção.

## Atualizações e desinstalação

O código atual não implementa atualização automática. Novas versões são distribuídas por instaladores publicados nas [Releases](https://github.com/ryanoviski/Hyperion/releases).

Para remover o aplicativo, use a área de aplicativos instalados do Windows. Antes disso, crie um backup e preserve `%APPDATA%\Hyperion` se quiser manter banco, anexos e histórico.

## Contribuindo

O Hyperion é um projeto de portfólio. Sugestões e correções podem ser abertas pelo GitHub como *issue* ou *pull request*. Antes de propor alterações, execute os testes relevantes e mantenha a documentação alinhada ao comportamento do código.

## Licença

Este projeto é distribuído sob a [Licença MIT](LICENSE).
