CREATE TABLE rules (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  priority INT NOT NULL,
  match_type TEXT NOT NULL,
  pattern TEXT NOT NULL,
  category_id BIGINT REFERENCES category(id) ON DELETE SET NULL,
  subcategory_id BIGINT,
  account_id BIGINT REFERENCES account(id) ON DELETE SET NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_rules_user_priority ON rules (user_id, priority);

CREATE TABLE budgets (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  month_ref TEXT NOT NULL,
  category_id BIGINT REFERENCES category(id) ON DELETE SET NULL,
  subcategory_id BIGINT,
  limit_cents BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id, month_ref, category_id, subcategory_id)
);

CREATE INDEX idx_budgets_user_month ON budgets (user_id, month_ref);

CREATE TABLE alerts (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type TEXT NOT NULL,
  month_ref TEXT NOT NULL,
  budget_id BIGINT NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
  message TEXT NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  triggered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id, budget_id, month_ref, type)
);

CREATE INDEX idx_alerts_user_month ON alerts (user_id, month_ref);

ALTER TABLE txn
  ADD COLUMN IF NOT EXISTS categorization_mode TEXT;

ALTER TABLE txn
  ADD CONSTRAINT fk_txn_rule FOREIGN KEY (rule_id) REFERENCES rules(id) ON DELETE SET NULL;

CREATE INDEX idx_txn_user_category_month ON txn (user_id, category_id, month_ref);
CREATE INDEX idx_txn_user_subcategory_month ON txn (user_id, subcategory_id, month_ref);
