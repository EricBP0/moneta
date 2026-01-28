# API — Contrato (v1)

Base URL: /api

## Headers padrão
- Content-Type: application/json
- Authorization: Bearer <accessToken> (obrigatório em /me e demais endpoints protegidos)

## Erros padrão
- 400 Bad Request: { "error": "VALIDATION_ERROR", "message": "..." }
- 401 Unauthorized: { "error": "UNAUTHORIZED", "message": "Token inválido/ausente" }
- 403 Forbidden: { "error": "FORBIDDEN", "message": "Sem acesso ao recurso" }
- 404 Not Found: { "error": "NOT_FOUND", "message": "Recurso não encontrado" }

## Auth
POST /auth/register
Request:
{ "email":"", "name":"", "password":"" }
Response:
{ "accessToken":"...", "refreshToken":"...", "user": { "id":1, "email":"", "name":"" } }

POST /auth/login
Request:
{ "email":"", "password":"" }
Response:
{ "accessToken":"...", "refreshToken":"...", "user": { "id":1, "email":"", "name":"" } }

POST /auth/refresh (opcional se adotado refresh token)
Request:
{ "refreshToken": "..." }
Response:
{ "accessToken":"...", "refreshToken":"..." }

GET /me
Response:
{ "id":1, "email":"", "name":"" }


## Institutions
GET /institutions
POST /institutions
Request: { "name":"Nubank", "type":"BANK" }
PATCH /institutions/{id}
DELETE /institutions/{id} (soft delete)


## Accounts
GET /accounts
POST /accounts
Request:
{
  "institutionId": 1,
  "name": "Itaú PJ",
  "type": "CHECKING",
  "currency": "BRL",
  "initialBalanceCents": 0
}

Response (inclui saldo calculado):
{
  "id": 1,
  "institutionId": 1,
  "name": "Itaú PJ",
  "type": "CHECKING",
  "currency": "BRL",
  "initialBalanceCents": 0,
  "balanceCents": 1500,
  "isActive": true
}

GET /accounts/{id}
PATCH /accounts/{id}
DELETE /accounts/{id}  (soft delete)


## Categories/Subcategories
GET /categories
POST /categories
PATCH /categories/{id}
DELETE /categories/{id}

GET /categories/{id}/subcategories
POST /categories/{id}/subcategories
PATCH /subcategories/{id}
DELETE /subcategories/{id}


## Transactions
Notas:
- Transferência é registrada como duas transações com o mesmo transferGroupId.
- txnType: NORMAL, TRANSFER, CARD_PURCHASE, CARD_PAYMENT.
- Todos os txns com status POSTED afetam saldo (incluindo CARD_PURCHASE); CARD_PAYMENT afeta saldo e pode quitar card_bill.

GET /txns?month=YYYY-MM&accountId=&categoryId=&q=&direction=&status=
Response:
[
  {
    "id": 1,
    "accountId": 10,
    "occurredAt": "2026-01-27T10:15:30Z",
    "description": "POSTO SHELL",
    "amountCents": 25000,
    "direction": "OUT",
    "monthRef": "2026-01",
    "status": "POSTED",
    "txnType": "NORMAL",
    "categoryId": 3,
    "subcategoryId": 8,
    "ruleId": 4,
    "transferGroupId": null,
    "isActive": true
  }
]

POST /txns
Request:
{
  "accountId": 10,
  "occurredAt": "2026-01-27T10:15:30Z",
  "description": "Posto Shell - gasolina",
  "amountCents": 25000,
  "direction": "OUT",
  "status": "POSTED",
  "categoryId": 3,
  "subcategoryId": 8
}

PATCH /txns/{id}
Request (exemplo):
{
  "accountId": 10,
  "occurredAt": "2026-01-27T10:15:30Z",
  "description": "Posto Shell - gasolina",
  "amountCents": 25000,
  "direction": "OUT",
  "status": "POSTED",
  "categoryId": 3,
  "subcategoryId": 8
}

DELETE /txns/{id}

POST /txns/transfer
Request:
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amountCents": 1000,
  "occurredAt": "2026-01-27T10:15:30Z",
  "description": "Transferência entre contas"
}


## Rules
GET /rules
POST /rules
Request:
{
  "name": "Uber",
  "priority": 10,
  "matchType": "CONTAINS",
  "pattern": "UBER",
  "accountId": null,
  "categoryId": 2,
  "subcategoryId": 5
}

POST /rules/apply
Request:
{
  "from": "2026-01-01",
  "to": "2026-01-31",
  "onlyUncategorized": true
}
Response:
{ "updatedCount": 123 }

PATCH /rules/{id}
DELETE /rules/{id}


## Budgets (Tetos)
GET /budgets?month=YYYY-MM
POST /budgets
Request:
{
  "monthRef": "2026-02-01",
  "categoryId": 1,
  "subcategoryId": 2,
  "limitCents": 120000
}
DELETE /budgets/{id}


## Goals
GET /goals
POST /goals
Request:
{
  "name": "Reserva de emergência",
  "targetAmountCents": 3000000,
  "targetDate": "2027-07",
  "startDate": "2026-02-01",
  "monthlyRateBps": 0
}

Response:
{
  "id": 1,
  "name": "Reserva de emergência",
  "targetAmountCents": 3000000,
  "startDate": "2026-02-01",
  "targetDate": "2027-07",
  "monthlyRateBps": 0,
  "status": "ACTIVE",
  "savedSoFarCents": 0
}

GET /goals/{id}
PATCH /goals/{id}
DELETE /goals/{id} (cancela via status)

GET /goals/{id}/contributions
POST /goals/{id}/contributions
Request:
{ "contributedAt":"2026-02-10", "amountCents": 100000, "note":"aporte mês" }

GET /goals/{id}/projection?asOf=YYYY-MM
Response:
{
  "savedSoFarCents": 400000,
  "targetAmountCents": 3000000,
  "monthsRemaining": 18,
  "neededMonthlyCents": 144444,
  "estimatedCompletionMonth": "2027-07",
  "schedule": [
    { "month": "2026-02", "savedProjectedCents": 544444, "neededMonthlyCents": 144444 }
  ]
}


## Dashboard
GET /dashboard/monthly?month=YYYY-MM
Response:
{
  "month": "2026-02",
  "incomeCents": 0,
  "expenseCents": 0,
  "netCents": 0,
  "byCategory": [ { "categoryId": 1, "categoryName": "Mercado", "expenseCents": 123000 } ],
  "budgetStatus": [
    {
      "budgetId": 10,
      "categoryId": 1,
      "subcategoryId": null,
      "limitCents": 200000,
      "consumptionCents": 123000,
      "percent": 0.615,
      "triggered80": true,
      "triggered100": false
    }
  ],
  "alerts": [ { "type":"BUDGET_80", "message":"..." } ],
  "goalsSummary": [
    {
      "goalId": 1,
      "name": "Reserva de emergência",
      "savedSoFarCents": 400000,
      "targetAmountCents": 3000000,
      "percent": 0.13,
      "neededMonthlyCents": 144444,
      "targetDate": "2027-07-01",
      "status": "ACTIVE"
    }
  ]
}

## Exemplos cURL
Criar meta:
curl -X POST /api/goals \\
  -H \"Authorization: Bearer <accessToken>\" \\
  -H \"Content-Type: application/json\" \\
  -d '{\"name\":\"Reserva\",\"targetAmountCents\":3000000,\"targetDate\":\"2027-07\",\"monthlyRateBps\":0}'

Adicionar aporte:
curl -X POST /api/goals/1/contributions \\
  -H \"Authorization: Bearer <accessToken>\" \\
  -H \"Content-Type: application/json\" \\
  -d '{\"contributedAt\":\"2026-02-10\",\"amountCents\":100000,\"note\":\"aporte\"}'

Projeção da meta:
curl -X GET \"/api/goals/1/projection?asOf=2026-02\" \\
  -H \"Authorization: Bearer <accessToken>\"

Dashboard mensal:
curl -X GET \"/api/dashboard/monthly?month=2026-02\" \\
  -H \"Authorization: Bearer <accessToken>\"


## Import (CSV) — MVP
POST /api/import/csv (multipart/form-data)
Campos:
- file: CSV
- accountId: number (ID numérico da conta)
CSV obrigatório: date, description, amount
CSV opcional: category, subcategory
Regras:
- date no formato YYYY-MM-DD
- amount < 0 => OUT (amountCents = abs)
- amount > 0 => IN
- amount == 0 => linha inválida
- category resolvida por nome (case-insensitive) no usuário; se não existir, mantém NULL; subcategory não é resolvida por nome na versão atual

Resposta:
{ "batchId": 1, "filename": "extrato.csv", "totals": { ... }, "status": "PARSED" }

GET /api/import/batches
GET /api/import/batches/{id}
GET /api/import/batches/{id}/rows?status=READY|ERROR|DUPLICATE&page=&size=
POST /api/import/batches/{id}/commit
Payload:
{
  "applyRulesAfterCommit": true,
  "skipDuplicates": true,
  "commitOnlyReady": true
}
DELETE /api/import/batches/{id}
Regra: não permite delete de batch COMMITTED.

## Alerts (in-app)
GET /alerts
PATCH /alerts/{id}
Request:
{ "isRead": true }

## Changelog
- Atualizei payloads de contas/transações para saldo inicial, txnType, transferGroupId e auditoria.
- Ampliei o contrato de import CSV com batches/rows, revisão, commit e dedupe por hash.
