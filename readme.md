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

## Principais alterações em relação ao projeto original


Esta versão evolui o projeto original mantendo a arquitetura em camadas

- **Perfis de acesso reais.** Antes, todo usuário autenticado recebia o perfil `USER` fixo no código. Agora existem ADMIN, GERENTE e ATENDENTE, com permissões verificadas no backend.
- **Pedido completo.** O pedido deixou de ter um único sabor em texto livre e passou a ter itens, quantidades, preços, desconto, total, responsável e data/hora de entrada. O valor é calculado no servidor.
- **Fluxo de status controlado.** Transições inválidas são bloqueadas, e a entrada e a saída registram data/hora e responsável.
- **Auditoria persistente.** Toda alteração em pedidos e clientes é registrada com usuário, perfil, data/hora e valores anteriores e novos.
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

Testam o fluxo de status e as regras do `PedidoService` (cálculo do total, transições, saída, cancelamento e limites do desconto), sem necessidade de banco de dados.

### 6. Testando a API

A API aceita autenticação **HTTP Basic**. Exemplos com `curl`:

```bash
# Sem autenticação: 401
curl -i http://localhost:8080/api/pedidos

# ATENDENTE tentando aplicar desconto: 403
curl -i -u atendente01:SENHA -H "Content-Type: application/json" \
     -d '{"percentual":10}' http://localhost:8080/api/pedidos/ID_DO_PEDIDO/desconto

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