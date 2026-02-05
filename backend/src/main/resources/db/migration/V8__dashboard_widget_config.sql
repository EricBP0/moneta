-- Create table for dashboard widget configuration
CREATE TABLE dashboard_widget_config (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  widget_key TEXT NOT NULL,
  is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  display_order INT NOT NULL DEFAULT 0,
  settings_json JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ
);

-- Create unique index to prevent duplicate widget_key per user
CREATE UNIQUE INDEX idx_dashboard_widget_config_user_widget 
  ON dashboard_widget_config (user_id, widget_key);

-- Create index for efficient queries by user
CREATE INDEX idx_dashboard_widget_config_user_order 
  ON dashboard_widget_config (user_id, display_order);
