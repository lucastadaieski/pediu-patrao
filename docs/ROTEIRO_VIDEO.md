# Roteiro do vídeo (até 3 minutos)

**Objetivo:** mostrar o dashboard funcionando e justificar as principais decisões de análise e projeto.
**Total previsto:** 2 min 50 s. Fale em ritmo normal; o texto de cada cena cabe no tempo indicado.

## Antes de gravar

- [ ] Aplicação rodando com dados (use `APP_DEMO_CARREGAR=true` no `.env` na primeira execução).
- [ ] Usuários `gerente01` e `atendente01` criados.
- [ ] Abas abertas: (1) dashboard logado como GERENTE, (2) aba anônima na tela de login, (3) terminal na pasta do projeto, (4) IntelliJ com `DashboardCalculadora` aberta, (5) GitHub na lista de branches.
- [ ] `./mvnw test` já executado uma vez (para não esperar o download durante a gravação).
- [ ] Zoom do navegador em 90% para caber o painel inteiro.

## Cenas

| Tempo | Tela | O que mostrar | O que falar |
|---|---|---|---|
| **0:00 a 0:15** | Dashboard | Painel completo | "Este é o Dashboard Administrativo do Pediu Patrão. Ele foi feito para o dono ou o gerente da pizzaria acompanhar as vendas, os pedidos e as tendências, consumindo uma API que eu projetei e implementei." |
| **0:15 a 0:40** | Dashboard, cards do topo | Passar o mouse nos 4 indicadores | "No topo estão os indicadores gerais: faturamento, pedidos, ticket médio e taxa de cancelamento. Cada um é comparado com o período anterior de mesmo tamanho, porque a pergunta do gestor é 'estou melhor ou pior?'. Uma regra importante da análise: pedido cancelado não é venda. Ele não entra no faturamento nem no ticket, só na taxa de cancelamento." |
| **0:40 a 1:05** | Gráficos | Trocar "Por dia" para "Por semana"; passar o mouse no gráfico por hora | "Nos gráficos vejo a evolução das vendas por dia, semana ou mês, o horário de pico e o dia mais movimentado, e a divisão entre entrega e retirada. Os períodos sem venda aparecem com zero, e quando uma semana está cortada pelo filtro a tela avisa, para não parecer uma queda." |
| **1:05 a 1:25** | Situação e ranking | Mostrar atrasados; trocar o ranking para Faturamento | "Aqui está a situação dos pedidos: quantos estão em cada status, quantos estão atrasados há mais de 40 minutos e o tempo médio de atendimento. Ao lado, o ranking de produtos, por quantidade ou por faturamento." |
| **1:25 a 1:40** | Filtros | Clicar em "7 dias" e depois filtrar "Entrega" | "Os filtros de período, status e atendimento valem para o painel inteiro. A tela atualiza sozinha a cada 60 segundos, e cada card carrega de forma independente: se um falhar, os outros continuam." |
| **1:40 a 2:05** | Terminal | Rodar `./docs/evidencias/testar-api.sh` e mostrar o resultado | "A API tem nove endpoints GET em /api/dashboard, todos documentados com parâmetros, exemplos e erros. Este script testa os endpoints com o banco real: respostas certas, 401 sem login, 403 para o atendente e 400 para parâmetros inválidos, como data inicial depois da final." |
| **2:05 a 2:30** | IntelliJ | Mostrar `DashboardController`, `DashboardService`, `DashboardCalculadora` e rodar `./mvnw test` | "No projeto, separei as responsabilidades: o controller só recebe os parâmetros, o service filtra os pedidos no MongoDB usando um índice por data, e a calculadora concentra as regras de negócio sem depender de Spring nem de banco. Por isso consegui cobrir todos os cálculos com testes unitários." |
| **2:30 a 2:45** | GitHub | Branches `main`, `develop`, `feature/dashboard-api` e os commits | "O desenvolvimento seguiu o Gitflow: a implementação foi feita na branch feature/dashboard-api, integrada na develop e depois na main." |
| **2:45 a 2:50** | Dashboard | Painel completo | "Esse é o dashboard funcionando de ponta a ponta. Obrigado." |

## Se precisar cortar tempo

Corte primeiro a cena do GitHub (fale do Gitflow em uma frase durante a cena do IntelliJ) e depois reduza a cena dos filtros.

## Depois de gravar

1. Suba no YouTube como **Não listado** ou no Google Drive com "Qualquer pessoa com o link".
2. Teste o link numa aba anônima.
3. Coloque o link no topo da `docs/DOCUMENTACAO.md` e do `readme.md`.
