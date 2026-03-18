ALTER TABLE work_score
  ADD COLUMN IF NOT EXISTS level INT,
  ADD COLUMN IF NOT EXISTS status VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_work_score_week_status
  ON work_score(week_no, status);

CREATE INDEX IF NOT EXISTS idx_work_score_week_level
  ON work_score(week_no, level);
