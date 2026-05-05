DROP TABLE IF EXISTS attendance_holiday_calendar CASCADE;
CREATE TABLE IF NOT EXISTS attendance_holiday_calendar (
    id BIGSERIAL PRIMARY KEY,
    calendar_date DATE NOT NULL,
    day_type VARCHAR(32) NOT NULL,
    name VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    remark VARCHAR(255),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_attendance_holiday_calendar_date
    ON attendance_holiday_calendar(calendar_date);

CREATE INDEX IF NOT EXISTS idx_attendance_holiday_calendar_day_type
    ON attendance_holiday_calendar(day_type);

CREATE INDEX IF NOT EXISTS idx_attendance_holiday_calendar_enabled
    ON attendance_holiday_calendar(enabled);

ALTER TABLE attendance_holiday_calendar
    ADD COLUMN IF NOT EXISTS calendar_year INT;

ALTER TABLE attendance_holiday_calendar
    ADD COLUMN IF NOT EXISTS source VARCHAR(32) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE attendance_holiday_calendar
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED';

UPDATE attendance_holiday_calendar
SET calendar_year = EXTRACT(YEAR FROM calendar_date)::INT
WHERE calendar_year IS NULL;

CREATE INDEX IF NOT EXISTS idx_attendance_holiday_calendar_year
    ON attendance_holiday_calendar(calendar_year);

CREATE INDEX IF NOT EXISTS idx_attendance_holiday_calendar_status
    ON attendance_holiday_calendar(status);

ALTER TABLE attendance_holiday_calendar
    DROP CONSTRAINT IF EXISTS chk_attendance_holiday_calendar_day_type;

ALTER TABLE attendance_holiday_calendar
    ADD CONSTRAINT chk_attendance_holiday_calendar_day_type
    CHECK (day_type IN ('HOLIDAY', 'WEEKDAY_REST', 'MAKEUP_WORKDAY')) NOT VALID;

ALTER TABLE attendance_holiday_calendar
    DROP CONSTRAINT IF EXISTS chk_attendance_holiday_calendar_source;

ALTER TABLE attendance_holiday_calendar
    ADD CONSTRAINT chk_attendance_holiday_calendar_source
    CHECK (source IN ('MANUAL', 'AI')) NOT VALID;

ALTER TABLE attendance_holiday_calendar
    DROP CONSTRAINT IF EXISTS chk_attendance_holiday_calendar_status;

ALTER TABLE attendance_holiday_calendar
    ADD CONSTRAINT chk_attendance_holiday_calendar_status
    CHECK (status IN ('DRAFT', 'CONFIRMED')) NOT VALID;
