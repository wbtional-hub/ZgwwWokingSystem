ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS login_fail_count INT NOT NULL DEFAULT 0;

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS lock_until TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_sys_user_lock_until
    ON sys_user(lock_until);

CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(64),
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    login_ip VARCHAR(64),
    login_result VARCHAR(16) NOT NULL,
    fail_reason VARCHAR(255),
    user_agent VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_sys_login_log_user_time
    ON sys_login_log(user_id, login_time DESC);

CREATE INDEX IF NOT EXISTS idx_sys_login_log_username_time
    ON sys_login_log(username, login_time DESC);
