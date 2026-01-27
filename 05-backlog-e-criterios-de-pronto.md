# Backlog e Critérios de Pronto

## Backlog (Sprints)

### Sprint 1 — Base
- [ ] Auth (register/login/refresh opcional + /me)
- [ ] CRUD Institutions
- [ ] CRUD Accounts
- [ ] CRUD Categories/Subcategories
- [ ] CRUD Transactions + filtros por mês
- [ ] Dashboard mensal básico (totais + por categoria)

### Sprint 2 — Regras
- [ ] CRUD Rules
- [ ] Apply Rules (lote)
- [ ] Tela de revisão: transações “uncategorized” e correção manual
- [ ] Seed: categorias e regras iniciais (uber/ifood/shell etc.)

### Sprint 3 — Orçamentos e alertas
- [ ] CRUD Budgets
- [ ] Cálculo de progresso por categoria/subcategoria
- [ ] Geração de alertas 80%/100%
- [ ] Tela de orçamento (teto + progresso)

### Sprint 4 — Metas e projeção até 07/2027
- [ ] CRUD Goals + contributions
- [ ] Projeção mensal
- [ ] Simulação Poupança vs Tesouro Selic (taxas editáveis)
- [ ] Tela de metas + progresso

### Sprint 5 — Import
- [ ] Import CSV
- [ ] Import OFX
- [ ] Dedupe por externalSource/externalId
- [ ] Tela de import + resultado do lote


## Critérios de pronto (Definition of Done)

### Para cada endpoint
- Validação de request (400 com mensagem clara)
- Retornos padronizados
- Autorização por usuário (nunca vazar dados de outro user)
- Responder 401 para token ausente/inválido e 403 para acesso proibido
- Teste unitário ou teste de integração (mínimo para serviços críticos)

### Para regras e orçamento (mínimo obrigatório)
- RuleService:
  - aplica por prioridade
  - não sobrescreve MANUAL (default)
  - marca categorizationMode=RULE
- BudgetService:
  - calcula outCents por categoria no mês
  - calcula pct
  - cria alertas 80% e 100% uma única vez por mês/categoria

### Para cada tela (React)
- Loading / Empty / Error states
- Filtros funcionam
- Totais batem com backend

### Para autenticação (mínimo obrigatório)
- Endpoints públicos limitados a /auth/register, /auth/login (e /auth/refresh se adotado)
- /me e demais endpoints exigem Authorization: Bearer <token>
- user_id deve ser derivado do JWT (sem X-User-Id)

## Changelog
- Atualizei o Sprint 1 para explicitar o fluxo de auth com refresh opcional.
- Incluí critérios de pronto para 401/403 e regras mínimas de autenticação com user_id via JWT.
