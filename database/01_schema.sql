CREATE TABLE IF NOT EXISTS sys_unit (
  id BIGSERIAL PRIMARY KEY,
  unit_name VARCHAR(100) NOT NULL,
  unit_code VARCHAR(50) NOT NULL UNIQUE,
  status SMALLINT NOT NULL DEFAULT 1,
  admin_user_id BIGINT,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGSERIAL PRIMARY KEY,
  unit_id BIGINT NOT NULL,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  real_name VARCHAR(64) NOT NULL,
  job_title VARCHAR(64),
  mobile VARCHAR(20),
  status SMALLINT NOT NULL DEFAULT 1,
  parent_user_id BIGINT,
  level_no INT,
  tree_path VARCHAR(500),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_user VARCHAR(64) NOT NULL DEFAULT 'system',
  update_user VARCHAR(64) NOT NULL DEFAULT 'system',
  is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.id IS '主键ID';
COMMENT ON COLUMN sys_user.unit_id IS '所属单位ID';
COMMENT ON COLUMN sys_user.username IS '登录账号';
COMMENT ON COLUMN sys_user.password_hash IS '加密后的登录密码';
COMMENT ON COLUMN sys_user.real_name IS '用户姓名';
COMMENT ON COLUMN sys_user.job_title IS '岗位名称，仅用于展示';
COMMENT ON COLUMN sys_user.mobile IS '手机号';
COMMENT ON COLUMN sys_user.status IS '状态：1启用，0停用';
COMMENT ON COLUMN sys_user.parent_user_id IS '上级用户ID';
COMMENT ON COLUMN sys_user.level_no IS '层级号';
COMMENT ON COLUMN sys_user.tree_path IS '树路径，格式如 /1/2/3/';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';
COMMENT ON COLUMN sys_user.create_user IS '创建人';
COMMENT ON COLUMN sys_user.update_user IS '更新人';
COMMENT ON COLUMN sys_user.is_deleted IS '逻辑删除标记：false未删除，true已删除';

CREATE TABLE IF NOT EXISTS sys_org_node (
  id BIGSERIAL PRIMARY KEY,
  unit_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL UNIQUE,
  parent_user_id BIGINT,
  level_no INT NOT NULL,
  tree_path VARCHAR(500) NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_code VARCHAR(32) NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, role_code)
);

CREATE INDEX IF NOT EXISTS idx_sys_org_node_unit_parent ON sys_org_node(unit_id, parent_user_id);
CREATE INDEX IF NOT EXISTS idx_sys_org_node_unit_path ON sys_org_node(unit_id, tree_path);
CREATE INDEX IF NOT EXISTS idx_sys_user_unit_id ON sys_user(unit_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_parent_user_id ON sys_user(parent_user_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_unit_tree_path ON sys_user(unit_id, tree_path);
CREATE INDEX IF NOT EXISTS idx_sys_user_status ON sys_user(status);
CREATE INDEX IF NOT EXISTS idx_sys_user_real_name ON sys_user(real_name);
CREATE INDEX IF NOT EXISTS idx_sys_user_mobile ON sys_user(mobile);
CREATE INDEX IF NOT EXISTS idx_sys_user_deleted_created ON sys_user(is_deleted, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_user_id ON sys_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_role_code ON sys_user_role(role_code);

CREATE TABLE IF NOT EXISTS attendance_record (
  id BIGSERIAL PRIMARY KEY,
  unit_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  check_type VARCHAR(16) NOT NULL,
  check_time TIMESTAMP NOT NULL,
  longitude NUMERIC(12,7),
  latitude NUMERIC(12,7),
  address VARCHAR(255),
  distance_meter NUMERIC(8,2),
  valid_flag SMALLINT NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attendance_record_unit_user_time ON attendance_record(unit_id, user_id, check_time);
CREATE INDEX IF NOT EXISTS idx_attendance_record_user_type_time ON attendance_record(user_id, check_type, check_time);

CREATE TABLE IF NOT EXISTS weekly_work (
  id BIGSERIAL PRIMARY KEY,
  unit_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  week_no VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  work_plan TEXT,
  work_content TEXT,
  remark TEXT,
  submit_time TIMESTAMP,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_weekly_work_user_week ON weekly_work(user_id, week_no);
CREATE INDEX IF NOT EXISTS idx_weekly_work_unit_user_status ON weekly_work(unit_id, user_id, status);
CREATE INDEX IF NOT EXISTS idx_weekly_work_week_no ON weekly_work(week_no);
