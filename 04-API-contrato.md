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
{ "accessToken":"...", "user": { "id":1, "email":"", "name":"" } }

POST /auth/login
Request:
{ "email":"", "password":"" }
Response:
{ "accessToken":"...", "user": { "id":1, "email":"", "name":"" } }

POST /auth/refresh (opcional se adotado refresh token)
Request:
{ "refreshToken": "..." }
Response:
{ "accessToken":"..." }

GET /me
Response:
{ "id":1, "email":"", "name":"" }


## Institutions
GET /institutions
POST /institutions
Request: { "name":"Nubank", "code":"NU" }


## Accounts
GET /accounts
POST /accounts
Request:
{
  "institutionId": 1,
  "name": "Itaú PJ",
  "type": "CHECKING",
  "currency": "BRL"
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
GET /txns?month=YYYY-MM&accountId=&categoryId=&q=&direction=
Response:
{
  "items": [
    {
      "id": 1,
      "accountId": 10,
      "occurredAt": "2026-01-27",
      "description": "POSTO SHELL",
      "amountCents": 25000,
      "direction": "OUT",
      "categoryId": 3,
      "subcategoryId": 8,
      "categorizationMode": "RULE"
    }
  ],
  "totals": { "inCents": 0, "outCents": 25000, "netCents": -25000 }
}

POST /txns
Request:
{
  "accountId": 10,
  "occurredAt": "2026-01-27",
  "description": "Posto Shell - gasolina",
  "amountCents": 25000,
  "direction": "OUT",
  "categoryId": 3,
  "subcategoryId": 8
}

PATCH /txns/{id}
Request (exemplo):
{ "categoryId": 3, "subcategoryId": 8, "categorizationMode": "MANUAL" }

DELETE /txns/{id}


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
  "name": "Casamento/Reforma",
  "goalType": "WEDDING_REFORM",
  "targetCents": 3000000,
  "startDate": "2026-02-01",
  "targetDate": "2027-07-01",
  "monthlyContribCents": 100000
}

GET /goals/{id}/contributions
POST /goals/{id}/contributions
Request:
{ "contributedAt":"2026-02-10", "amountCents": 100000, "note":"aporte mês" }


## Dashboard
GET /dashboard/monthly?month=YYYY-MM
Response:
{
  "month": "2026-02",
  "totals": { "inCents": 0, "outCents": 0, "netCents": 0 },
  "byCategory": [ { "categoryId": 1, "outCents": 123000 } ],
  "budgetStatus": [ { "categoryId": 1, "limitCents": 200000, "outCents": 123000, "pct": 0.615 } ],
  "alerts": [ { "type":"BUDGET_80", "message":"..." } ]
}


## Import (CSV/OFX) — MVP
POST /import/csv (multipart)
POST /import/ofx (multipart)
GET /import/batches
GET /import/batches/{id}

## Changelog
- Adicionei headers padrão e formato de erros (incluindo 401/403) para o contrato da API.
- Incluí o endpoint opcional de refresh token e clarifiquei a proteção de endpoints via Authorization Bearer.
