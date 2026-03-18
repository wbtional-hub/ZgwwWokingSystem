-- 修复 sys_user 与 UserMapper.xml 字段不一致
-- 目标：补齐 update_time / create_user / update_user / is_deleted
-- 兼容 PostgreSQL

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS update_time TIMESTAMP;

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS create_user VARCHAR(64);

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS update_user VARCHAR(64);

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE sys_user
SET update_time = COALESCE(update_time, create_time),
    create_user = COALESCE(create_user, 'system'),
    update_user = COALESCE(update_user, 'system'),
    is_deleted = COALESCE(is_deleted, FALSE)
WHERE update_time IS NULL
   OR create_user IS NULL
   OR update_user IS NULL
   OR is_deleted IS NULL;
