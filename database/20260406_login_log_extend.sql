ALTER TABLE sys_login_log
    ADD COLUMN IF NOT EXISTS login_type VARCHAR(20);

ALTER TABLE sys_login_log
    ADD COLUMN IF NOT EXISTS device_type VARCHAR(20);
