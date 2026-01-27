# Banco de Dados — PostgreSQL (DDL)

## Convenções
- IDs: BIGSERIAL
- Datas de negócio: DATE
- Timestamps técnicos: TIMESTAMPTZ default now()
- Valores: BIGINT em centavos

```sql
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  name TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE refresh_token (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash TEXT NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE institution (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  code TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id, name)
);

CREATE TABLE account (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  institution_id BIGINT REFERENCES institution(id),
  name TEXT NOT NULL,
  type TEXT NOT NULL, -- CHECKING, SAVINGS, CREDIT_CARD, WALLET, INVESTMENT
  currency TEXT NOT NULL DEFAULT 'BRL',
  initial_balance_cents BIGINT NOT NULL DEFAULT 0,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  external_source TEXT,
  external_id TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id, external_source, external_id)
);

CREATE TABLE category (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  color TEXT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id, name)
);

CREATE TABLE subcategory (
  id BIGSERIAL PRIMARY KEY,
  category_id BIGINT NOT NULL REFERENCES category(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(category_id, name)
);

CREATE TABLE import_batch (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  source TEXT NOT NULL, -- CSV
  filename TEXT,
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at TIMESTAMPTZ,
  status TEXT NOT NULL DEFAULT 'RUNNING', -- RUNNING/SUCCESS/FAILED
  error_message TEXT
);

CREATE TABLE import_row (
  id BIGSERIAL PRIMARY KEY,
  import_batch_id BIGINT NOT NULL REFERENCES import_batch(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  row_index INT NOT NULL,
  row_hash TEXT NOT NULL,
  raw_payload JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id, row_hash)
);

CREATE TABLE card_bill (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE, -- type=CREDIT_CARD
  month_ref DATE NOT NULL, -- use 1º dia do mês
  due_date DATE,
  closed_at DATE,
  amount_cents BIGINT NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'OPEN', -- OPEN/CLOSED/PAID
  paid_at DATE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id, account_id, month_ref)
);

CREATE TABLE rule (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  priority INT NOT NULL DEFAULT 100,
  match_type TEXT NOT NULL, -- CONTAINS | STARTS_WITH | REGEX
  pattern TEXT NOT NULL,
  account_id BIGINT REFERENCES account(id),
  category_id BIGINT REFERENCES category(id),
  subcategory_id BIGINT REFERENCES subcategory(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_rule_user_priority ON rule(user_id, priority);

CREATE TABLE txn (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
  occurred_at DATE NOT NULL,
  posted_at DATE,
  description_raw TEXT NOT NULL,
  description_norm TEXT NOT NULL,
  amount_cents BIGINT NOT NULL,
  direction TEXT NOT NULL, -- IN/OUT
  txn_type TEXT NOT NULL DEFAULT 'STANDARD', -- STANDARD, TRANSFER, CARD_PURCHASE, CARD_PAYMENT
  currency TEXT NOT NULL DEFAULT 'BRL',
  card_bill_id BIGINT REFERENCES card_bill(id) ON DELETE SET NULL,
  category_id BIGINT REFERENCES category(id),
  subcategory_id BIGINT REFERENCES subcategory(id),
  categorization_mode TEXT NOT NULL DEFAULT 'UNCATEGORIZED',
  rule_id BIGINT REFERENCES rule(id),
  transfer_group_id UUID,
  external_source TEXT,
  external_id TEXT,
  import_batch_id BIGINT REFERENCES import_batch(id) ON DELETE SET NULL,
  import_row_id BIGINT REFERENCES import_row(id) ON DELETE SET NULL,
  status TEXT NOT NULL DEFAULT 'POSTED',
  categorized_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id, external_source, external_id)
);

CREATE INDEX idx_txn_user_date ON txn(user_id, occurred_at);
CREATE INDEX idx_txn_user_account_date ON txn(user_id, account_id, occurred_at);
CREATE INDEX idx_txn_user_category_date ON txn(user_id, category_id, occurred_at);

CREATE TABLE budget (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  month_ref DATE NOT NULL, -- 1º dia do mês
  category_id BIGINT NOT NULL REFERENCES category(id),
  subcategory_id BIGINT REFERENCES subcategory(id),
  limit_cents BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id, month_ref, category_id, subcategory_id)
);

CREATE TABLE goal (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  goal_type TEXT NOT NULL, -- EMERGENCY, WEDDING_REFORM, OTHER
  target_cents BIGINT NOT NULL,
  start_date DATE NOT NULL,
  target_date DATE NOT NULL,
  monthly_contrib_cents BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE goal_contribution (
  id BIGSERIAL PRIMARY KEY,
  goal_id BIGINT NOT NULL REFERENCES goal(id) ON DELETE CASCADE,
  contributed_at DATE NOT NULL,
  amount_cents BIGINT NOT NULL,
  note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE alert (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type TEXT NOT NULL, -- BUDGET_80, BUDGET_100, GOAL_BEHIND etc.
  month_ref DATE,
  category_id BIGINT REFERENCES category(id),
  message TEXT NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

## Changelog
- Adicionei saldo inicial em account para cálculo de saldo derivado.
- Incluí import_row e ajuste de import_batch para CSV, com dedupe por row_hash.
- Acrescentei campos de auditoria em txn (rule_id, import_row_id, categorized_at) e suporte a transfer_group_id e txn_type.
- Ampliei card_bill com paid_at para refletir pagamento de fatura.
