CREATE TABLE IF NOT EXISTS sys_org_unit (
  id BIGSERIAL PRIMARY KEY,
  unit_name VARCHAR(100) NOT NULL,
  unit_code VARCHAR(50) NOT NULL UNIQUE,
  status SMALLINT NOT NULL DEFAULT 1,
  admin_user_id BIGINT,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_org_unit (id, unit_name, unit_code, status, admin_user_id, create_time)
SELECT id, unit_name, unit_code, status, admin_user_id, create_time
FROM sys_unit
ON CONFLICT (id) DO UPDATE SET
  unit_name = EXCLUDED.unit_name,
  unit_code = EXCLUDED.unit_code,
  status = EXCLUDED.status,
  admin_user_id = EXCLUDED.admin_user_id;

CREATE INDEX IF NOT EXISTS idx_sys_org_unit_status
  ON sys_org_unit(status);
