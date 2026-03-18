CREATE TABLE IF NOT EXISTS work_score (
  id BIGSERIAL PRIMARY KEY,
  week_no VARCHAR(20) NOT NULL,
  unit_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  attendance_days INT NOT NULL DEFAULT 0,
  attendance_score NUMERIC(6,2) NOT NULL DEFAULT 0,
  weekly_work_status VARCHAR(20),
  weekly_work_score NUMERIC(6,2) NOT NULL DEFAULT 0,
  discipline_score NUMERIC(6,2) NOT NULL DEFAULT 20,
  discipline_remark VARCHAR(255),
  total_score NUMERIC(6,2) NOT NULL DEFAULT 0,
  calculate_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, week_no)
);

CREATE INDEX IF NOT EXISTS idx_work_score_week_unit ON work_score(week_no, unit_id);
CREATE INDEX IF NOT EXISTS idx_work_score_week_total ON work_score(week_no, total_score DESC);
