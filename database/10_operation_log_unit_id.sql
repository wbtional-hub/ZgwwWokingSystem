ALTER TABLE operation_log
  ADD COLUMN IF NOT EXISTS unit_id BIGINT;

UPDATE operation_log log
SET unit_id = user_info.unit_id
FROM sys_user user_info
WHERE log.operator_id = user_info.id
  AND log.unit_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_operation_log_unit_time
  ON operation_log(unit_id, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_operation_log_unit_module_time
  ON operation_log(unit_id, module_name, create_time DESC);
