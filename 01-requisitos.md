# Requisitos (Funcionais e Não Funcionais)

## RF-01 — Autenticação
- Cadastro, login e sessão via JWT.
- Endpoints públicos: /auth/register, /auth/login (e /auth/refresh se adotado).
- Endpoints protegidos: /me e todos os demais.
- user_id sempre derivado do JWT (sem X-User-Id ou usuário default).
- Critérios de aceite:
  - POST /api/auth/login retorna accessToken
  - Rotas protegidas exigem Authorization: Bearer <token>
- Prioridade: P1

## RF-02 — Contas (Accounts)
- CRUD de contas: CHECKING, SAVINGS, CREDIT_CARD, WALLET, INVESTMENT
- Critérios:
  - Criar/editar/desativar conta
  - Conta pode ter instituição (Nubank, Itaú, BTG etc.)
- Prioridade: P1

## RF-03 — Transações (Txns)
- CRUD de transações.
- Campos mínimos:
  - accountId, occurredAt (date), description, amountCents, direction (IN/OUT)
- Filtros:
  - por mês, conta, categoria, texto (q), direction
- Critérios:
  - amountCents sempre positivo
  - direction define se entra ou sai
- Prioridade: P1

## RF-04 — Categorias/Subcategorias
- CRUD de categorias e subcategorias.
- Prioridade: P1

## RF-05 — Regras de categorização automática
- CRUD de regras:
  - matchType: CONTAINS | STARTS_WITH | REGEX
  - pattern
  - priority (menor aplica primeiro)
  - opcional: restringir por accountId
  - define categoryId/subcategoryId
- Aplicação em lote:
  - aplicar em range de datas e/ou apenas uncategorized
- Critérios:
  - Por padrão, não sobrescreve transação categorizada MANUAL
  - marca categorizationMode = RULE
- Prioridade: P1

## RF-06 — Orçamento / Tetos
- Definir teto por mês (monthRef) e categoria/subcategoria.
- Dashboard mostra:
  - gasto do mês, teto, percentual
- Alertas:
  - 80% e 100% do teto (notificação interna)
- Prioridade: P1

## RF-07 — Metas (Goals) e Projeção até 07/2027
- Criar metas: Emergência, Casamento/Reforma
- Registrar aportes (manual)
- Projeção mensal com aporte planejado
- Simulação: Poupança vs Tesouro Selic (taxas editáveis)
- Prioridade: P2

## RF-08 — Dashboard mensal
- Resumo do mês:
  - Entradas, saídas, saldo estimado, gasto por categoria
- Prioridade: P1


## RNF — Requisitos Não Funcionais
- RNF-01: Backend em Java + Spring Boot
- RNF-02: Front em React.js (SPA) e compatível com PWA
- RNF-03: Banco PostgreSQL
- RNF-04: Values em centavos (BIGINT) + direction IN/OUT
- RNF-05: Logs sem dados sensíveis
- RNF-06: Testes unitários para regras e orçamento
- RNF-07: Docker Compose para ambiente local
- RNF-08: Autorização sempre baseada no user_id do JWT

## Changelog
- Detalhei endpoints públicos/protegidos e a origem do user_id via JWT no requisito de autenticação.
- Adicionei requisito não funcional reforçando autorização baseada no user_id do JWT.
