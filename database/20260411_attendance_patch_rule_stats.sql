CREATE TABLE IF NOT EXISTS attendance_patch_apply (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  unit_id BIGINT NOT NULL,
  attendance_date DATE NOT NULL,
  patch_type VARCHAR(16) NOT NULL,
  patch_time TIMESTAMP NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  approve_user_id BIGINT,
  approve_time TIMESTAMP,
  approve_comment VARCHAR(500),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  valid_flag SMALLINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_attendance_patch_apply_user_date
  ON attendance_patch_apply(user_id, attendance_date DESC);

CREATE INDEX IF NOT EXISTS idx_attendance_patch_apply_status_date
  ON attendance_patch_apply(status, attendance_date DESC, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_attendance_patch_apply_unit_status
  ON attendance_patch_apply(unit_id, status, create_time DESC);

CREATE TABLE IF NOT EXISTS attendance_rule (
  id BIGSERIAL PRIMARY KEY,
  unit_id BIGINT NOT NULL,
  work_start_time TIME NOT NULL,
  work_end_time TIME NOT NULL,
  late_grace_minutes INT NOT NULL DEFAULT 0,
  early_leave_grace_minutes INT NOT NULL DEFAULT 0,
  status SMALLINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_attendance_rule_unit
  ON attendance_rule(unit_id);

CREATE INDEX IF NOT EXISTS idx_attendance_rule_status
  ON attendance_rule(status, update_time DESC);
