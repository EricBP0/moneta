-- Create cards table for credit card management
CREATE TABLE card (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  brand TEXT,
  last4 VARCHAR(4),
  limit_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
  closing_day INT NOT NULL CHECK (closing_day BETWEEN 1 AND 31),
  due_day INT NOT NULL CHECK (due_day BETWEEN 1 AND 31),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ
);

-- Create indexes for efficient queries
CREATE INDEX idx_card_user_id ON card (user_id);
CREATE INDEX idx_card_account_id ON card (account_id);
CREATE UNIQUE INDEX idx_card_user_name ON card (user_id, name) WHERE is_active = TRUE;

-- Add payment type and card relationship to transactions
ALTER TABLE txn
  ADD COLUMN payment_type TEXT,
  ADD COLUMN card_id BIGINT REFERENCES card(id) ON DELETE SET NULL;

-- Create index for card transactions
CREATE INDEX idx_txn_card_id ON txn (card_id);

-- Set default payment_type for existing transactions to PIX
UPDATE txn SET payment_type = 'PIX' WHERE payment_type IS NULL;

-- Make payment_type required
ALTER TABLE txn ALTER COLUMN payment_type SET NOT NULL;

-- Add check constraints to enforce payment type rules
-- PIX transactions must have account_id and not have card_id
-- CARD transactions must have card_id and account_id must be NULL
ALTER TABLE txn ADD CONSTRAINT chk_txn_payment_type_pix
  CHECK (
    payment_type != 'PIX' OR 
    (payment_type = 'PIX' AND card_id IS NULL AND account_id IS NOT NULL)
  );

ALTER TABLE txn ADD CONSTRAINT chk_txn_payment_type_card
  CHECK (
    payment_type != 'CARD' OR 
    (payment_type = 'CARD' AND card_id IS NOT NULL AND account_id IS NULL)
  );

-- Add index for invoice queries (card transactions by date range)
CREATE INDEX idx_txn_card_occurred ON txn (card_id, occurred_at DESC) WHERE payment_type = 'CARD' AND is_active = TRUE;
