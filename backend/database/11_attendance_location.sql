CREATE TABLE IF NOT EXISTS attendance_location (
    id BIGSERIAL PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    location_name VARCHAR(100) NOT NULL,
    latitude NUMERIC(10, 6) NOT NULL,
    longitude NUMERIC(10, 6) NOT NULL,
    radius_meters INTEGER NOT NULL DEFAULT 200,
    address VARCHAR(255),
    status INTEGER NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_attendance_location_unit_id
    ON attendance_location (unit_id);

CREATE INDEX IF NOT EXISTS idx_attendance_location_status
    ON attendance_location (status);
