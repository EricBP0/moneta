CREATE TABLE goals (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  target_amount_cents BIGINT NOT NULL,
  start_date DATE NOT NULL,
  target_date DATE NOT NULL,
  monthly_rate_bps INT NOT NULL DEFAULT 0,
  status TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE goal_contributions (
  id BIGSERIAL PRIMARY KEY,
  goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  contributed_at DATE NOT NULL,
  amount_cents BIGINT NOT NULL,
  note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_goal_contributions_user_goal_date
  ON goal_contributions (user_id, goal_id, contributed_at);

ALTER TABLE alerts
  ALTER COLUMN budget_id DROP NOT NULL;

ALTER TABLE alerts
  ADD COLUMN goal_id BIGINT REFERENCES goals(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX uniq_alerts_goal_month_type
  ON alerts (user_id, goal_id, month_ref, type)
  WHERE goal_id IS NOT NULL;

CREATE INDEX idx_alerts_user_month_goal ON alerts (user_id, month_ref, goal_id);
