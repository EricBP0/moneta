-- Drop NOT NULL constraint from categorization_mode to allow uncategorized transactions
-- This enables the application to distinguish between manually categorized transactions
-- (categorization_mode = 'MANUAL') and uncategorized transactions (categorization_mode IS NULL)
-- for proper rule application logic

ALTER TABLE txn ALTER COLUMN categorization_mode DROP NOT NULL;

-- Update existing transactions that are truly uncategorized (no category, subcategory, or rule)
-- to have NULL categorization_mode instead of the default 'MANUAL'
UPDATE txn 
SET categorization_mode = NULL 
WHERE category_id IS NULL 
  AND subcategory_id IS NULL 
  AND rule_id IS NULL 
  AND categorization_mode = 'MANUAL';
