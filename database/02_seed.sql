-- 角色示例
-- SUPER_ADMIN: 超级管理员
-- UNIT_ADMIN: 单位管理员
-- ORG_USER: 组织用户

INSERT INTO sys_unit (id, unit_name, unit_code, status, admin_user_id)
VALUES
  (1, '平台管理单位', 'PLATFORM', 1, 1),
  (2, '第一讲师团', 'UNIT_001', 1, 2)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_user (id, unit_id, username, password_hash, real_name, job_title, mobile, status, parent_user_id, level_no, tree_path)
VALUES
  (1, 1, 'admin', '$2a$10$rBMJmCW6ED0E6Q2M7QfPUu4A0M2lJvLCYOtJk9gDC11FXGeUXrXeS', '超级管理员', '平台管理员', '13800000001', 1, NULL, 1, '/1/'),
  (2, 2, 'unitadmin', '$2a$10$rBMJmCW6ED0E6Q2M7QfPUu4A0M2lJvLCYOtJk9gDC11FXGeUXrXeS', '单位管理员', '单位负责人', '13800000002', 1, NULL, 1, '/2/'),
  (3, 2, 'orguser1', '$2a$10$rBMJmCW6ED0E6Q2M7QfPUu4A0M2lJvLCYOtJk9gDC11FXGeUXrXeS', '一级讲师', '讲师', '13800000003', 1, 2, 2, '/2/3/'),
  (4, 2, 'orguser2', '$2a$10$rBMJmCW6ED0E6Q2M7QfPUu4A0M2lJvLCYOtJk9gDC11FXGeUXrXeS', '二级讲师', '助理讲师', '13800000004', 1, 3, 3, '/2/3/4/')
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_code)
VALUES
  (1, 'SUPER_ADMIN'),
  (2, 'UNIT_ADMIN'),
  (3, 'ORG_USER'),
  (4, 'ORG_USER')
ON CONFLICT (user_id, role_code) DO NOTHING;

-- 参数示例
-- default_password
-- attendance_valid_radius_meter
-- org_user_can_create_child
-- export_max_rows
