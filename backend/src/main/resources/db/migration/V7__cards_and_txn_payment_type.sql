CREATE TABLE cards (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  brand TEXT,
  last4 VARCHAR(4),
  limit_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
  closing_day INT NOT NULL,
  due_day INT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_cards_closing_day CHECK (closing_day BETWEEN 1 AND 31),
  CONSTRAINT chk_cards_due_day CHECK (due_day BETWEEN 1 AND 31)
);

CREATE INDEX idx_cards_user_id ON cards (user_id);
CREATE INDEX idx_cards_account_id ON cards (account_id);

ALTER TABLE txn
  ADD COLUMN payment_type TEXT NOT NULL DEFAULT 'PIX',
  ADD COLUMN card_id BIGINT REFERENCES cards(id) ON DELETE SET NULL;

ALTER TABLE txn
  ALTER COLUMN account_id DROP NOT NULL;

ALTER TABLE txn
  ADD CONSTRAINT chk_txn_payment_type
  CHECK (
    (payment_type = 'PIX' AND account_id IS NOT NULL AND card_id IS NULL)
    OR (payment_type = 'CARD' AND card_id IS NOT NULL AND account_id IS NULL)
  );

CREATE INDEX idx_txn_card_occurred ON txn (card_id, occurred_at);
