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
  - Transferência é representada por 2 transações ligadas por transferGroupId
- Status no MVP:
  - POSTED entra no saldo.
  - PENDING/CANCELED não entram no saldo.
  - Manual e import CSV criam POSTED por padrão.
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
  - 80% e 100% do teto (notificação interna/in-app)
- Prioridade: P1

## RF-07 — Metas (Goals) e Projeção até 07/2027
- Criar metas: Emergência, Casamento/Reforma
- Registrar aportes (manual)
- Projeção mensal com aporte planejado
- Simulação simples com juros default 0 (editável depois)
- Prioridade: P2

## RF-08 — Dashboard mensal
- Resumo do mês:
  - Entradas, saídas, saldo estimado, gasto por categoria
- Prioridade: P1

## RF-09 — Saldo de conta (derivado)
- Saldo = saldo_inicial + soma das transações com status POSTED/CONFIRMED.
- Transações com tipo CARD_PURCHASE não afetam saldo; CARD_PAYMENT afeta.
- Prioridade: P1

## RF-10 — Importação CSV (MVP)
- Importar CSV com colunas obrigatórias: date, description, amount (accountId no upload).
- Colunas opcionais: category, subcategory (resolver por nome, sem criar automaticamente).
- amount pode ser positivo/negativo; converter para amountCents + direction (zero é inválido).
- Criar import_batch + import_row com status PARSED/READY/ERROR/DUPLICATE e totals.
- Fluxo: upload -> review -> commit (gera txns com status POSTED).
- Dedupe por hash contra txns existentes e rows da mesma batch.
- Prioridade: P1

## RF-11 — Auditoria de categorização
- Guardar categorizationMode, ruleId (quando aplicado), importBatchId e importRowId opcional.
- Não sobrescrever MANUAL automaticamente em regras/aplicações.
- Manter timestamps relevantes (createdAt, categorizedAt quando houver).
- Prioridade: P1

## RF-12 — Cartão e fatura mínima
- CARD_PURCHASE gera transação vinculada a card_bill e não afeta saldo da conta.
- CARD_PAYMENT reduz saldo da conta e pode marcar card_bill como paga.
- Prioridade: P1

## RF-13 — Alertas (MVP)
- Alertas no MVP apenas para orçamento (80% e 100% do teto).
- Alertas de meta ficam como P2.
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
- RNF-09: MVP multiusuário com isolamento por user_id via JWT (sem multi-tenant/compartilhamento)

## Changelog
- Adicionei requisitos para saldo derivado, transferências, importação CSV e auditoria.
- Ajustei metas para simulação simples com juros default 0 e defini regras mínimas de cartão.
- Esclareci que alertas são in-app no MVP.
- Registrei o MVP como single-user, mantendo user_id para evolução futura.
