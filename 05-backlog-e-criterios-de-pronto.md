# Backlog e Critérios de Pronto

## Backlog (Sprints)

### Sprint 1 — Base
- [ ] Auth (register/login/refresh opcional + /me)
- [ ] CRUD Institutions
- [ ] CRUD Accounts
- [ ] CRUD Categories/Subcategories
- [ ] CRUD Transactions + filtros por mês
- [ ] Transferências (2 txns + transferGroupId)
- [ ] Regras mínimas de cartão (CARD_PURCHASE/CARD_PAYMENT)
- [ ] Dashboard mensal básico (totais + por categoria)

### Sprint 2 — Regras
- [ ] CRUD Rules
- [ ] Apply Rules (lote)
- [ ] Tela de revisão: transações “uncategorized” e correção manual
- [ ] Seed: categorias e regras iniciais (uber/ifood/shell etc.)
- [ ] Auditoria mínima (ruleId + importBatchId + timestamps)

### Sprint 3 — Orçamentos e alertas
- [ ] CRUD Budgets
- [ ] Cálculo de progresso por categoria/subcategoria
- [ ] Geração de alertas 80%/100% (apenas orçamento no MVP)
- [ ] Tela de orçamento (teto + progresso)

### Sprint 4 — Metas e projeção até 07/2027
- [ ] CRUD Goals + contributions
- [ ] Projeção mensal
- [ ] Simulação simples (juros default 0, editável depois)
- [ ] Tela de metas + progresso
- [ ] Alertas de metas (P2)

### Sprint 5 — Import
- [ ] Import CSV com colunas date, description, amount (+ opcionais account/category)
- [ ] Import batch + import_row
- [ ] Dedupe simples por hash
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
- Atualizei Sprint 1 e 2 para incluir transferências, regras de cartão e auditoria mínima.
- Ajustei o backlog de import para CSV-only com import_row e dedupe por hash.
- Simplifiquei a simulação de metas para juros default 0 no Sprint 4.
