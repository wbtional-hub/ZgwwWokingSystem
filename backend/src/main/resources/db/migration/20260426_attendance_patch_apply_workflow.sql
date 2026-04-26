ALTER TABLE attendance_patch_apply
    ADD COLUMN IF NOT EXISTS apply_type VARCHAR(32);

UPDATE attendance_patch_apply
SET apply_type = 'MAKEUP'
WHERE apply_type IS NULL
   OR BTRIM(apply_type) = '';

ALTER TABLE attendance_patch_apply
    ALTER COLUMN apply_type SET DEFAULT 'MAKEUP';

CREATE INDEX IF NOT EXISTS idx_attendance_patch_apply_type_status
    ON attendance_patch_apply(apply_type, status, attendance_date DESC);

CREATE TABLE IF NOT EXISTS attendance_patch_apply_node (
    id BIGSERIAL PRIMARY KEY,
    apply_id BIGINT NOT NULL,
    node_order INT NOT NULL,
    node_code VARCHAR(64) NOT NULL,
    node_name VARCHAR(64) NOT NULL,
    approver_user_id BIGINT,
    approver_name VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'WAITING',
    approve_time TIMESTAMP,
    approve_comment VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_attendance_patch_apply_node_apply_order
    ON attendance_patch_apply_node(apply_id, node_order);

CREATE INDEX IF NOT EXISTS idx_attendance_patch_apply_node_apply_status
    ON attendance_patch_apply_node(apply_id, status, node_order);

CREATE INDEX IF NOT EXISTS idx_attendance_patch_apply_node_approver_status
    ON attendance_patch_apply_node(approver_user_id, status, apply_id);

CREATE TABLE IF NOT EXISTS attendance_patch_apply_log (
    id BIGSERIAL PRIMARY KEY,
    apply_id BIGINT NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    operator_user_id BIGINT,
    operator_name VARCHAR(64),
    node_code VARCHAR(64),
    node_name VARCHAR(64),
    comment VARCHAR(1000),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attendance_patch_apply_log_apply_time
    ON attendance_patch_apply_log(apply_id, create_time DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_attendance_patch_apply_log_operator
    ON attendance_patch_apply_log(operator_user_id, create_time DESC);
