# 🍕 PediuPatrão
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB_Atlas-47A248?style=for-the-badge&logo=mongodb&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![JUnit5](https://img.shields.io/badge/JUnit_5-25A162?style=for-the-badge&logo=junit5&logoColor=white)

---

## 📄 Documentação da atividade do Dashboard

| Entregável | Onde está |
|---|---|
| Requisitos funcionais e não funcionais, modelagem e projeto | [docs/DOCUMENTACAO.md](docs/DOCUMENTACAO.md) |
| Documentação dos endpoints (parâmetros, exemplos e erros) | [docs/DOCUMENTACAO.md, seção 6](docs/DOCUMENTACAO.md#6-documentação-dos-endpoints) |
| Evidências de testes | [docs/DOCUMENTACAO.md, seção 7](docs/DOCUMENTACAO.md#7-testes-e-evidências) e [docs/evidencias/](docs/evidencias/) |
| Requisições prontas | [docs/api-dashboard.http](docs/api-dashboard.http) (IntelliJ) e [docs/evidencias/testar-api.sh](docs/evidencias/testar-api.sh) |
| Branch da implementação (Gitflow) | `feature/dashboard-api` → `develop` → `main` |
| Vídeo de apresentação | _link do vídeo_ ([roteiro](docs/ROTEIRO_VIDEO.md)) |

---

## Principais alterações em relação ao projeto original


Esta versão evolui o projeto original mantendo a arquitetura em camadas

- **Perfis de acesso reais.** Antes, todo usuário autenticado recebia o perfil `USER` fixo no código. Agora existem ADMIN, GERENTE e ATENDENTE, com permissões verificadas no backend.
- **Pedido completo.** O pedido deixou de ter um único sabor em texto livre e passou a ter itens, quantidades, preços, desconto, total, responsável e data/hora de entrada. O valor é calculado no servidor.
- **Fluxo de status controlado.** Transições inválidas são bloqueadas, e a entrada e a saída registram data/hora e responsável.
- **Auditoria persistente.** Toda alteração em pedidos e clientes é registrada com usuário, perfil, data/hora e valores anteriores e novos.
- **Dashboard Administrativo.** Painel para ADMIN e GERENTE com indicadores de vendas, gráficos, situação dos pedidos, ranking de produtos e filtros, alimentado por uma API própria (`/api/dashboard`). Detalhes na seção [Dashboard Administrativo](#-dashboard-administrativo).
- **Correções do projeto original.** API de usuários aberta sem login, CSRF desligado, senha do banco no código, tela de produtos que não salvava, edição de usuário que corrompia a senha e falha de XSS na lista de clientes.

---

## Requisitos Implementados

### Usuários e perfis (RF01)

| Recurso | ADMIN | GERENTE | ATENDENTE |
|---|:-:|:-:|:-:|
| Gerenciar usuários e atribuir perfis | ✔ | | |
| Consultar pedidos | ✔ | ✔ | ✔ |
| Registrar pedidos e acompanhar o andamento | | ✔ | ✔ |
| **Aplicar desconto e cancelar pedido** | | **✔** | |
| Consultar clientes | | ✔ | ✔ |
| Cadastrar e alterar clientes | | | ✔ |
| Consultar a auditoria | ✔ | ✔ | |
| Acessar o dashboard | ✔ | ✔ | |
| Manter o cardápio (produtos) | ✔ | ✔ | |

### Pedidos (RF02)

- Pedido com **itens**, quantidades, preço unitário, subtotal, percentual e valor de desconto e **total**.
- O preço vem do cadastro do produto no momento do pedido e fica gravado no item. Se o preço mudar depois, o histórico do pedido não muda.
- **O total é sempre calculado no servidor.** A API recebe apenas cliente, tipo de atendimento e itens; qualquer `valorTotal` ou `status` enviado na requisição é ignorado.
- Produto inativo, produto inexistente e quantidade inválida são recusados.

### Fluxo de status (RF03)
```
RECEBIDO → EM PREPARAÇÃO → PRONTO → SAIU PARA ENTREGA (entrega)  → FINALIZADO
                                  → RETIRADO          (retirada) → FINALIZADO

CANCELADO: só a partir de RECEBIDO, EM PREPARAÇÃO ou PRONTO, somente pelo GERENTE
```

- Transições fora desse fluxo são bloqueadas (ex.: finalizar um pedido em preparação).
- Pedidos cancelados ou finalizados não aceitam mais nenhuma alteração de status.
- **Entrada** e **saída** registram data/hora e o usuário responsável, sempre obtido da sessão autenticada.




### Cancelamento e desconto (RF04)

- Exclusivos do **GERENTE**.
- **Cancelamento:** exige justificativa, só é permitido antes da saída, preserva todos os dados do pedido e impede alterações posteriores.
- **Desconto:** percentual de até **20%**, só antes da saída, com o total recalculado no servidor.

### Auditoria (RF05)

- Registra inclusões e alterações de pedidos, mudanças de status, descontos, cancelamentos e alterações de clientes (com destaque para telefone e endereço).
- Cada registro guarda: **usuário e perfil**, **data e hora**, **tipo de operação**, **entidade e identificador**, **campos alterados com valor anterior e novo**, e **justificativa** quando exigida.
- Também registra **tentativas de acesso negadas**, permitindo conferir as violações de permissão.
- Consulta pela tela **Auditoria** ou por `GET /api/auditoria`, somente para ADMIN e GERENTE.

---

## 📊 Dashboard Administrativo

Painel para o responsável pelo estabelecimento acompanhar as vendas, os pedidos e as tendências da operação. Acesso pelo menu **Dashboard**, somente para **ADMIN** e **GERENTE**.

### Análise: dos requisitos aos dados

Antes de criar os endpoints, levantei o que cada requisito precisa e de onde vem a informação. Todos os dados já existiam no `Pedido` (entrada, status, tipo de atendimento, itens e valores calculados no servidor), então **não foi preciso alterar a entidade**.

| Requisito | Pergunta que responde | Dados usados | Endpoint |
|---|---|---|---|
| **RF01** Indicadores gerais | Quanto vendi? Estou melhor ou pior que antes? | `valorTotal`, `status`, `valorDesconto`, `itens` | `GET /resumo` |
| **RF02** Análise de vendas | Como as vendas evoluem? Qual o horário e o dia de pico? Entrega ou retirada? | `dataHoraEntrada`, `valorTotal`, `tipoAtendimento` | `GET /vendas/serie`, `/vendas/por-hora`, `/vendas/por-dia-semana`, `/vendas/por-tipo-atendimento` |
| **RF03** Situação dos pedidos | Quantos estão em cada etapa? Algum atrasado? Quanto demora um pedido? | `status`, `dataHoraEntrada`, `dataHoraFinalizacao` | `GET /pedidos/situacao`, `/pedidos/recentes` |
| **RF04** Produtos mais vendidos | O que mais vende, em quantidade e em dinheiro? | `itens` (produto, quantidade, subtotal) + `Produto.tipo` | `GET /produtos/ranking` |
| **RF05** Filtros | Recortar por período, status e atendimento | parâmetros de consulta | todos + `GET /filtros` |
| **RF06** Integração com a API | Tela sem cálculo próprio | JSON da API | tela `dashboard.html` com `fetch` |
| **RF07** Atualização e tratamento | Dados atuais, erros claros, nada quebra com dado faltando | ver abaixo | todos |

### Regras de negócio

- **Pedido cancelado não é venda.** Não entra no faturamento, no ticket médio, nos itens vendidos, nos gráficos de venda nem no ranking. Entra só na contagem de cancelados e na **taxa de cancelamento**.
- **Ticket médio** = faturamento ÷ pedidos válidos (não cancelados).
- **Comparação com o período anterior:** cada indicador é comparado com um período de mesmo tamanho imediatamente antes (ex.: 01 a 10/11 comparado com 22 a 31/10). Se o período anterior não teve vendas, a variação fica `null` ("sem base"), em vez de um percentual infinito. A taxa de cancelamento é comparada em **pontos percentuais**.
- **Atrasado:** pedido ainda em andamento há mais de **40 minutos** desde a entrada (configurável em `DASHBOARD_MINUTOS_ATRASO`).
- **Tempo médio de atendimento:** da entrada à finalização, só dos pedidos finalizados.
- **Ranking:** soma o subtotal dos itens com o preço gravado no pedido. O desconto é dado no pedido inteiro, então não é rateado por produto. Produto excluído do cadastro continua aparecendo (como "Sem categoria"), porque o histórico não pode sumir.

### Endpoints da API

Todos são `GET`, exigem ADMIN ou GERENTE e aceitam os mesmos filtros opcionais:

| Parâmetro | Valores | Padrão |
|---|---|---|
| `inicio`, `fim` | data `AAAA-MM-DD` | últimos 30 dias, incluindo hoje |
| `status` | `RECEBIDO`, `EM_PREPARACAO`, `PRONTO`, `SAIU_PARA_ENTREGA`, `RETIRADO`, `FINALIZADO`, `CANCELADO` | todos |
| `tipoAtendimento` | `ENTREGA`, `RETIRADA` | todos |

| Endpoint | Retorno | Parâmetros extras |
|---|---|---|
| `GET /api/dashboard/resumo` | faturamento, pedidos, ticket médio, cancelamentos, itens, descontos e comparação com o período anterior | |
| `GET /api/dashboard/vendas/serie` | pedidos, cancelados, faturamento e ticket médio ao longo do tempo (períodos sem venda vêm com zero) | `agrupamento=DIA\|SEMANA\|MES` |
| `GET /api/dashboard/vendas/por-hora` | 24 faixas (0h a 23h) com pedidos, faturamento e participação | |
| `GET /api/dashboard/vendas/por-dia-semana` | segunda a domingo | |
| `GET /api/dashboard/vendas/por-tipo-atendimento` | entrega x retirada | |
| `GET /api/dashboard/pedidos/situacao` | quantidade por status, em andamento, atrasados, tempo médio | |
| `GET /api/dashboard/pedidos/recentes` | últimos pedidos do período, com indicação de atraso | `limite` (1 a 50, padrão 10) |
| `GET /api/dashboard/produtos/ranking` | posição, produto, categoria, quantidade, faturamento e participação | `criterio=QUANTIDADE\|FATURAMENTO`, `limite`, `categoria` |
| `GET /api/dashboard/filtros` | opções válidas de status, atendimento, agrupamento, critério e categorias, e o período padrão | |

Exemplo (valores ilustrativos) de resposta de `GET /api/dashboard/resumo?inicio=2026-11-01&fim=2026-11-10`:

```json
{
  "inicio": "2026-11-01", "fim": "2026-11-10",
  "faturamento": 8120.50, "totalPedidos": 84, "pedidosValidos": 78, "pedidosCancelados": 6,
  "taxaCancelamento": 7.1, "ticketMedio": 104.11, "itensVendidos": 231, "totalDescontos": 42.30,
  "comparativo": {
    "inicio": "2026-10-22", "fim": "2026-10-31",
    "faturamento": 7390.00, "pedidosValidos": 75, "ticketMedio": 98.53, "taxaCancelamento": 6.3,
    "variacaoFaturamento": 9.9, "variacaoPedidos": 4.0, "variacaoTicketMedio": 5.7,
    "diferencaTaxaCancelamento": 0.8
  },
  "atualizadoEm": "2026-11-10T20:15:03"
}
```

Erros seguem o mesmo formato do restante da API (`ErroResponse`):

| Situação | Resposta |
|---|---|
| Sem login | `401` |
| ATENDENTE acessando o dashboard | `403` (e fica registrado na auditoria) |
| `inicio` depois de `fim`, período maior que 366 dias, `limite` fora de 1 a 50 | `400` com a mensagem da regra |
| Valor inválido (`status=XYZ`, data em outro formato) | `400` com os valores aceitos |

### Projeto da solução

```
dashboard.html (JS + Chart.js)
        │  fetch /api/dashboard/*  (filtros na query string)
        ▼
DashboardController ── só recebe e converte os parâmetros
        ▼
DashboardService ───── monta o FiltroDashboard e busca no MongoDB
        │                (só os pedidos do período, usando o índice de dataHoraEntrada)
        ▼
DashboardCalculadora ─ regras de negócio puras (sem Spring, sem banco)
        ▼
DTOs (records) ─────── ResumoVendas, PontoSerie, FatiaVendas, SituacaoPedidos, ProdutoRanking...
```

Decisões tomadas:

- **DTOs próprios em vez de devolver a entidade.** A API devolve só o que a tela precisa (ex.: `PedidoRecente` não expõe responsáveis nem justificativas).
- **Filtro no banco, cálculo no Java.** O período, o status e o atendimento são filtrados no MongoDB, com um índice em `dataHoraEntrada` criado no `MongoInitConfig`. Os indicadores são calculados em Java, na `DashboardCalculadora`, porque assim as regras ficam legíveis e testáveis sem banco. Para o volume de uma pizzaria (algumas centenas de pedidos por mês, período máximo de 1 ano) isso é rápido. Se o volume crescer muito, o caminho natural é trocar a calculadora por *aggregation pipelines* do MongoDB, sem mudar o contrato da API.
- **Endpoints separados por assunto.** Cada card da tela chama o seu endpoint. Se um falhar, só aquele card mostra erro e o resto continua funcionando. Também permite trocar o agrupamento do gráfico ou o critério do ranking sem recarregar tudo.
- **`FiltroDashboard` valida a si mesmo.** É um `record` que recusa período inválido no construtor, então nenhum código recebe um filtro inconsistente.

### Atualização e tratamento de dados (RF07)

- Atualização automática a cada **60 segundos** (pode ser desligada), pausada quando a aba não está visível, e botão **Atualizar**.
- Horário da última atualização no topo da tela, vindo do servidor.
- Estados de **carregando**, **vazio** ("nenhum pedido no período") e **erro** em cada card, de forma independente.
- Validação das datas na tela e de novo na API.
- Séries e distribuições sempre completas: dias, horas e status sem pedidos aparecem com zero, para o gráfico não "pular".
- Aviso quando o agrupamento por semana ou mês tem um período cortado pelo filtro, para não parecer queda de vendas.
- Dados antigos ou incompletos não quebram o cálculo: valor nulo conta como zero, cliente sem nome aparece como "(sem nome)", item sem `produtoId` é agrupado pelo nome.
- Os filtros ficam na URL, então dá para recarregar a página ou mandar o link sem perder a consulta.

### Dados de demonstração

Para os gráficos não ficarem vazios num banco novo, existe uma carga opcional. No `.env`:

```properties
APP_DEMO_CARREGAR=true
```

Na próxima execução, o `DemoDataLoader` cria cerca de 500 pedidos nos últimos 60 dias, com mais movimento de sexta a domingo e no jantar, cerca de 7% de cancelamentos e alguns descontos. Se não houver produtos ou clientes, ele cria um cardápio e clientes de exemplo. A carga roda **uma única vez** (os pedidos ficam com responsável `demo`). Depois volte a variável para `false`.

Esses pedidos são gravados direto no banco, sem passar pelo `PedidoService`, por isso não geram auditoria. Para removê-los:

```js
db.pedidos.deleteMany({ responsavelEntrada: "demo" })
```

---

## ▶️ Como executar o projeto

### Pré-requisitos

- **JDK 21**
- Conta no **MongoDB Atlas** com um cluster, um usuário de banco e o seu IP liberado em *Network Access*
- O Maven **não** precisa estar instalado: o projeto usa o Maven Wrapper (`mvnw`)

### 1. Clonar o repositório

```bash
git clone https://github.com/lucastadaieski/pediu-patrao.git
cd pediu-patrao
```

### 2. Configurar as variáveis de ambiente

Copie o `.env.example` para `.env` na raiz do projeto e preencha:

```properties
MONGODB_URI=mongodb+srv://USUARIO:SENHA@SEU_CLUSTER.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0
MONGODB_DATABASE=pediupatrao
ADMIN_USERNAME=admin
ADMIN_PASSWORD=uma-senha-forte
```

### 3. Executar

```bash
./mvnw spring-boot:run          # Linux / macOS / Git Bash
mvnw.cmd spring-boot:run        # Windows (CMD ou PowerShell)
```

Ou execute a classe `PizzariaPedidosApplication` pela IDE. No primeiro start, o console mostra `Usuário administrador 'admin' criado.`

### 4. Primeiro acesso

1. Acesse `http://localhost:8080` e entre com o usuário e a senha definidos no `.env`.
2. Em **Configurações > Usuários**, crie um usuário **GERENTE** e um **ATENDENTE**.
3. Como GERENTE, cadastre os produtos em **Configurações > Produtos**.
4. Como ATENDENTE, cadastre clientes e registre pedidos.

### 5. Testes

```bash
./mvnw test
```

Testam o fluxo de status e as regras do `PedidoService` (cálculo do total, transições, saída, cancelamento e limites do desconto) e, no dashboard, os filtros de período (`FiltroDashboardTest`), todos os indicadores, séries e o ranking (`DashboardCalculadoraTest`) e a coerência dos dados de demonstração (`DemoDataLoaderTest`). Nenhum teste precisa de banco de dados.

### 6. Testando a API

A API aceita autenticação **HTTP Basic**. Exemplos com `curl`:

```bash
# Sem autenticação: 401
curl -i http://localhost:8080/api/pedidos

# ATENDENTE tentando aplicar desconto: 403
curl -i -u atendente01:SENHA -H "Content-Type: application/json" \
     -d '{"percentual":10}' http://localhost:8080/api/pedidos/ID_DO_PEDIDO/desconto

# GERENTE consultando o resumo do dashboard em novembro, só entregas
curl -u gerente01:SENHA "http://localhost:8080/api/dashboard/resumo?inicio=2026-11-01&fim=2026-11-30&tipoAtendimento=ENTREGA"

# ATENDENTE tentando ver o dashboard: 403
curl -i -u atendente01:SENHA http://localhost:8080/api/dashboard/resumo

# GERENTE consultando a auditoria de um pedido
curl -u gerente01:SENHA "http://localhost:8080/api/auditoria?entidade=Pedido&entidadeId=ID_DO_PEDIDO"
```

| Endpoint | Descrição | Perfis |
|---|---|---|
| `GET /api/pedidos` | Lista pedidos (filtro `?status=`) | Todos |
| `POST /api/pedidos` | Registra pedido | GERENTE, ATENDENTE |
| `PUT /api/pedidos/{id}/status?status=` | Avança o status | GERENTE, ATENDENTE |
| `POST /api/pedidos/{id}/desconto` | Aplica desconto (até 20%) | GERENTE |
| `POST /api/pedidos/{id}/cancelar` | Cancela com justificativa | GERENTE |
| `GET /api/clientes` | Lista clientes | GERENTE, ATENDENTE |
| `POST`/`PUT /api/clientes` | Cadastra / altera cliente | ATENDENTE |
| `GET /api/auditoria` | Consulta a auditoria | ADMIN, GERENTE |
| `/api/usuarios` | Gerencia usuários e perfis | ADMIN |
| `GET /api/dashboard/*` | Dados do dashboard (ver seção Dashboard) | ADMIN, GERENTE |
