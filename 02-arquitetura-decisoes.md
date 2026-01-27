# Arquitetura e Decisões

## Stack
- Backend: Java 21 + Spring Boot
- DB: PostgreSQL
- Front: React.js + (Chart.js/Recharts) para gráficos
- Auth: JWT
- Deploy local: Docker Compose (API + Postgres)

## Padrões de domínio
- amountCents (BIGINT) sempre positivo
- direction: IN | OUT (sem números negativos)
- descriptionNorm: uppercase, sem acentos, trim, espaços normalizados
- Soft delete: accounts/categories (isActive=false)

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

## Segurança
- Não armazenar credenciais bancárias
- Tokens (se houver Open Finance no futuro) criptografados em repouso
- CORS restrito ao front
- Rate limit simples (opcional)
