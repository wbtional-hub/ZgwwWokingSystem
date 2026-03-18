ALTER TABLE sys_user
  ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

UPDATE sys_user
SET role = 'ADMIN'
WHERE username = 'admin';

UPDATE sys_user
SET role = 'USER'
WHERE role IS NULL OR TRIM(role) = '';
