CREATE TABLE IF NOT EXISTS operation_log (
  id BIGSERIAL PRIMARY KEY,
  module_name VARCHAR(64) NOT NULL,
  action_name VARCHAR(64) NOT NULL,
  operator_id BIGINT,
  operator_name VARCHAR(64) NOT NULL,
  biz_id BIGINT,
  content VARCHAR(500) NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_operation_log_module_time
  ON operation_log(module_name, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_operation_log_operator_time
  ON operation_log(operator_name, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_operation_log_create_time
  ON operation_log(create_time DESC);
