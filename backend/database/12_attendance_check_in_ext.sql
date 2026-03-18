ALTER TABLE attendance_record
    ADD COLUMN IF NOT EXISTS check_in_latitude NUMERIC(10, 6),
    ADD COLUMN IF NOT EXISTS check_in_longitude NUMERIC(10, 6),
    ADD COLUMN IF NOT EXISTS check_in_distance_meters INTEGER,
    ADD COLUMN IF NOT EXISTS check_in_result VARCHAR(32);
