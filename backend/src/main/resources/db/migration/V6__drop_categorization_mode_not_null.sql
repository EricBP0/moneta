-- Drop NOT NULL constraint from categorization_mode to allow uncategorized transactions
-- This enables the application to distinguish between manually categorized transactions
-- (categorization_mode = 'MANUAL') and uncategorized transactions (categorization_mode IS NULL)
-- for proper rule application logic

ALTER TABLE txn ALTER COLUMN categorization_mode DROP NOT NULL;
