-- 周报管理逐级审批流最小底座
-- 本轮只补审批状态模型、定向退回目标模型、审批日志模型

ALTER TABLE weekly_work
    ADD COLUMN IF NOT EXISTS current_approval_node VARCHAR(100) NULL COMMENT '当前审批节点：STAFF/SECTION_CHIEF/DEPUTY_LEADER/LEGION_LEADER/USER_x',
    ADD COLUMN IF NOT EXISTS last_return_target VARCHAR(100) NULL COMMENT '最近一次定向退回目标',
    ADD COLUMN IF NOT EXISTS last_return_comment VARCHAR(500) NULL COMMENT '最近一次退回意见',
    ADD COLUMN IF NOT EXISTS last_review_by BIGINT NULL COMMENT '最近一次审批人用户ID',
    ADD COLUMN IF NOT EXISTS last_review_time DATETIME NULL COMMENT '最近一次审批时间';

CREATE TABLE IF NOT EXISTS weekly_work_approval_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    weekly_work_id BIGINT NOT NULL COMMENT '周报ID',
    action VARCHAR(32) NOT NULL COMMENT '动作：APPROVE/RETURN',
    from_node VARCHAR(32) NULL COMMENT '审批来源节点',
    to_node VARCHAR(32) NULL COMMENT '审批目标节点',
    reviewer_user_id BIGINT NOT NULL COMMENT '审批人用户ID',
    reviewer_name VARCHAR(64) NULL COMMENT '审批人姓名',
    comment VARCHAR(500) NULL COMMENT '审批意见',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_weekly_work_approval_log_work_id (weekly_work_id),
    CONSTRAINT fk_weekly_work_approval_log_work_id
        FOREIGN KEY (weekly_work_id) REFERENCES weekly_work (id)
) COMMENT='周报审批日志';
