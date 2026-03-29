CREATE TABLE IF NOT EXISTS sys_user_module_permission (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  module_code VARCHAR(64) NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64) NOT NULL DEFAULT 'system',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) NOT NULL DEFAULT 'system',
  CONSTRAINT uk_sys_user_module_permission UNIQUE (user_id, module_code)
);

CREATE INDEX IF NOT EXISTS idx_sys_user_module_permission_user_id
  ON sys_user_module_permission(user_id);

CREATE INDEX IF NOT EXISTS idx_sys_user_module_permission_module_code
  ON sys_user_module_permission(module_code);
