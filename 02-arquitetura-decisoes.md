# Arquitetura e Decisões

## Stack
- Backend: Java 21 + Spring Boot
- DB: PostgreSQL
- Front: React.js + (Chart.js/Recharts) para gráficos
- Auth: JWT
- Deploy local: Docker Compose (API + Postgres)

## Multiusuário (MVP)
- MVP single-user, mantendo user_id nas tabelas para evolução futura.
- user_id sempre derivado do JWT (sem header X-User-Id ou usuário default).
- Endpoints públicos: /auth/register e /auth/login (e /auth/refresh se adotado).
- /me e todos os demais endpoints são protegidos.

## Padrões de domínio
- amountCents (BIGINT) sempre positivo
- direction: IN | OUT (sem números negativos)
- descriptionNorm: uppercase, sem acentos, trim, espaços normalizados
- Soft delete: accounts/categories (isActive=false)
- Saldo de conta derivado: saldo_inicial + soma das transações POSTED/CONFIRMED
- Transferências são duas transações ligadas por transferGroupId
- Transações de cartão:
  - CARD_PURCHASE não afeta saldo de conta
  - CARD_PAYMENT afeta saldo e pode quitar card_bill

## Categorização
- Transação tem categoryId/subcategoryId (opcional)
- categorizationMode:
  - UNCATEGORIZED | RULE | MANUAL | ML
- Regras aplicam por:
  - priority asc (menor primeiro)
  - matchType: CONTAINS, STARTS_WITH, REGEX
- Regra não sobrescreve MANUAL por padrão (configurável no futuro)

## Dedupe (import/sync)
- externalSource + externalId únicos por usuário
- Import cria importBatch para auditoria
- MVP: import CSV com import_row e dedupe simples por hash

## Alertas
- Apenas in-app via tabela/endpoint de alerts (sem e-mail/push no MVP)

## Metas e projeção
- Simulação simples com juros default 0 (editável depois)

## Auditoria
- Transações registram categorizationMode, ruleId e importBatchId
- timestamps para criação e categorização (quando houver)

## Segurança
- Não armazenar credenciais bancárias
- Tokens (se houver Open Finance no futuro) criptografados em repouso
- CORS restrito ao front
- Rate limit simples (opcional)
- Se usar refresh token, armazenar apenas hash + expiração + revogação.

## Changelog
- Ajustei a decisão para MVP single-user com user_id preservado nas tabelas.
- Documentei saldo derivado, transferências, regras de cartão, import CSV e dedupe por hash.
- Registrei alertas in-app, simulação simples de metas e campos mínimos de auditoria.
