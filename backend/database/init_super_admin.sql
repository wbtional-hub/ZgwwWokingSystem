-- 超级管理员最小初始化脚本
-- 说明：
-- 1. 先初始化 sys_user
-- 2. 再初始化 sys_user_role
-- 3. 默认 unit_id = 1，如你的库中不是这个单位，请先改成已有 unit_id

INSERT INTO sys_user (
    unit_id,
    username,
    password_hash,
    real_name,
    job_title,
    mobile,
    status,
    create_time,
    update_time,
    create_user,
    update_user,
    is_deleted
)
SELECT
    1,
    'admin',
    '$2a$10$./CtQ3JKbARrXIIttSinhujWx8mfpdZUK9M4gn6zp/5lTeYEAHJSu',
    '超级管理员',
    '系统管理员',
    '13800000000',
    1,
    NOW(),
    NOW(),
    'system',
    'system',
    FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'admin'
      AND is_deleted = FALSE
);

INSERT INTO sys_user_role (user_id, role_code)
SELECT u.id, 'SUPER_ADMIN'
FROM sys_user u
WHERE u.username = 'admin'
  AND u.is_deleted = FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user_role r
      WHERE r.user_id = u.id
        AND r.role_code = 'SUPER_ADMIN'
  );
