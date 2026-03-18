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
  admin_user_id = EXCLUDED.admin_user_id,
  create_time = EXCLUDED.create_time;

ALTER TABLE sys_user
  ADD COLUMN IF NOT EXISTS unit_id BIGINT;

ALTER TABLE sys_org_node
  ADD COLUMN IF NOT EXISTS unit_id BIGINT;

UPDATE sys_org_node node
SET unit_id = user_info.unit_id
FROM sys_user user_info
WHERE node.user_id = user_info.id
  AND (node.unit_id IS NULL OR node.unit_id <> user_info.unit_id);

CREATE INDEX IF NOT EXISTS idx_sys_org_unit_status_delivery
  ON sys_org_unit(status);

CREATE INDEX IF NOT EXISTS idx_sys_org_node_unit_id_delivery
  ON sys_org_node(unit_id);
