ALTER TABLE attendance_record
    ADD COLUMN IF NOT EXISTS check_in_fail_reason VARCHAR(255);
