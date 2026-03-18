-- 组织架构树最小闭环：改为直接使用 sys_user 存储树字段

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS parent_user_id BIGINT;

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS level_no INTEGER;

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS tree_path VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_sys_user_parent_user_id
    ON sys_user (parent_user_id);

CREATE INDEX IF NOT EXISTS idx_sys_user_unit_id_tree_path
    ON sys_user (unit_id, tree_path);

UPDATE sys_user u
SET parent_user_id = n.parent_user_id,
    level_no = n.level_no,
    tree_path = CASE
        WHEN RIGHT(n.tree_path, 1) = '/' THEN n.tree_path
        ELSE n.tree_path || '/'
    END
FROM sys_org_node n
WHERE u.id = n.user_id
  AND (u.parent_user_id IS NULL OR u.level_no IS NULL OR u.tree_path IS NULL);
