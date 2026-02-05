-- Add payment type and card support to import rows
ALTER TABLE import_row
  ADD COLUMN payment_type TEXT,
  ADD COLUMN parsed_card_name TEXT,
  ADD COLUMN parsed_account_name TEXT,
  ADD COLUMN resolved_card_id BIGINT REFERENCES card(id) ON DELETE SET NULL;

-- Set default payment_type for existing rows to PIX
UPDATE import_row SET payment_type = 'PIX' WHERE payment_type IS NULL;

-- Make payment_type required
ALTER TABLE import_row ALTER COLUMN payment_type SET NOT NULL;

-- Create index for card references
CREATE INDEX idx_import_row_resolved_card_id ON import_row (resolved_card_id);
