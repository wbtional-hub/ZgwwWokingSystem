ALTER TABLE attendance_record
  ADD COLUMN IF NOT EXISTS attendance_date DATE,
  ADD COLUMN IF NOT EXISTS check_in_time TIMESTAMP,
  ADD COLUMN IF NOT EXISTS check_out_time TIMESTAMP,
  ADD COLUMN IF NOT EXISTS check_in_address VARCHAR(255),
  ADD COLUMN IF NOT EXISTS check_out_address VARCHAR(255);

UPDATE attendance_record
SET attendance_date = COALESCE(attendance_date, DATE(check_time))
WHERE attendance_date IS NULL;

UPDATE attendance_record
SET check_in_time = COALESCE(check_in_time, check_time),
    check_in_address = COALESCE(check_in_address, address)
WHERE check_type = 'IN'
  AND check_in_time IS NULL;

UPDATE attendance_record target
SET check_out_time = source.check_time,
    check_out_address = COALESCE(source.address, target.check_out_address)
FROM attendance_record source
WHERE target.user_id = source.user_id
  AND target.attendance_date = DATE(source.check_time)
  AND target.check_type = 'IN'
  AND source.check_type = 'OUT'
  AND target.check_out_time IS NULL;

ALTER TABLE attendance_record
  ALTER COLUMN attendance_date SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_attendance_record_user_date
  ON attendance_record(user_id, attendance_date);

CREATE INDEX IF NOT EXISTS idx_attendance_record_unit_date
  ON attendance_record(unit_id, attendance_date DESC);
