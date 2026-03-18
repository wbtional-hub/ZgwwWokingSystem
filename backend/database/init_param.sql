-- 参数配置最小可用闭环

CREATE TABLE IF NOT EXISTS sys_param (
    id BIGSERIAL PRIMARY KEY,
    param_code VARCHAR(100) NOT NULL,
    param_name VARCHAR(200) NOT NULL,
    param_value TEXT,
    status INTEGER NOT NULL DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_user VARCHAR(100),
    update_user VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_param_code
    ON sys_param (param_code)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_sys_param_status
    ON sys_param (status);
