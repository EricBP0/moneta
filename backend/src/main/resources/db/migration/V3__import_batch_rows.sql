CREATE TABLE import_batch (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
  filename TEXT NOT NULL,
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  status TEXT NOT NULL,
  total_rows INTEGER NOT NULL DEFAULT 0,
  error_rows INTEGER NOT NULL DEFAULT 0,
  duplicate_rows INTEGER NOT NULL DEFAULT 0,
  ready_rows INTEGER NOT NULL DEFAULT 0,
  committed_rows INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE import_row (
  id BIGSERIAL PRIMARY KEY,
  batch_id BIGINT NOT NULL REFERENCES import_batch(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  row_index INTEGER NOT NULL,
  raw_line TEXT,
  parsed_date DATE,
  description TEXT,
  amount_cents BIGINT,
  direction TEXT,
  resolved_category_id BIGINT REFERENCES category(id) ON DELETE SET NULL,
  resolved_subcategory_id BIGINT,
  hash TEXT,
  status TEXT NOT NULL,
  error_message TEXT,
  created_txn_id BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_import_rows_user_batch_status ON import_row (user_id, batch_id, status);
CREATE UNIQUE INDEX idx_import_rows_user_hash ON import_row (user_id, hash);

ALTER TABLE txn
  ADD COLUMN import_row_id BIGINT REFERENCES import_row(id) ON DELETE SET NULL,
  ADD COLUMN categorization_mode TEXT NOT NULL DEFAULT 'MANUAL';

CREATE INDEX idx_txn_user_import_batch ON txn (user_id, import_batch_id);
