ALTER TABLE institution
  ADD COLUMN IF NOT EXISTS type TEXT,
  ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE txn (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
  amount_cents BIGINT NOT NULL,
  direction TEXT NOT NULL,
  description TEXT,
  occurred_at TIMESTAMPTZ NOT NULL,
  month_ref TEXT NOT NULL,
  status TEXT NOT NULL,
  txn_type TEXT NOT NULL,
  category_id BIGINT REFERENCES category(id) ON DELETE SET NULL,
  subcategory_id BIGINT,
  rule_id BIGINT,
  import_batch_id BIGINT,
  transfer_group_id UUID,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_txn_user_month ON txn (user_id, month_ref);
CREATE INDEX idx_txn_user_account_occurred ON txn (user_id, account_id, occurred_at);
CREATE INDEX idx_txn_transfer_group ON txn (transfer_group_id);
CREATE INDEX idx_txn_posted_active_balance
  ON txn (user_id, account_id)
  WHERE status = 'POSTED' AND is_active = TRUE;
