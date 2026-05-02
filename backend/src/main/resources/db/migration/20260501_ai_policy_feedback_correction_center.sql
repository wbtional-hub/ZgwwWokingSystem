CREATE TABLE IF NOT EXISTS ai_policy_feedback_task (
    id BIGSERIAL PRIMARY KEY,
    feedback_id BIGINT,
    base_id BIGINT,
    user_id BIGINT,
    session_id BIGINT,
    message_id BIGINT,
    trace_id VARCHAR(128),
    answer_log_id BIGINT,
    question TEXT,
    answer TEXT,
    feedback_type VARCHAR(64),
    feedback_content TEXT,
    policy_key VARCHAR(128),
    policy_name VARCHAR(255),
    topic_type VARCHAR(128),
    question_type VARCHAR(128),
    evidence_ids TEXT,
    evidence_source TEXT,
    route_plan TEXT,
    validation_summary TEXT,
    status VARCHAR(64) NOT NULL DEFAULT 'PENDING',
    priority INTEGER NOT NULL DEFAULT 100,
    handler_id BIGINT,
    handle_result TEXT,
    handled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_policy_feedback_task_status
    ON ai_policy_feedback_task(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_policy_feedback_task_policy
    ON ai_policy_feedback_task(policy_key, topic_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_policy_feedback_task_session
    ON ai_policy_feedback_task(session_id, message_id);

CREATE TABLE IF NOT EXISTS ai_policy_feedback_action (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    action_type VARCHAR(64),
    action_content TEXT,
    draft_type VARCHAR(64),
    draft_payload TEXT,
    target_table VARCHAR(128),
    target_id BIGINT,
    operator_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_policy_feedback_action_task
    ON ai_policy_feedback_action(task_id, created_at DESC);
