CREATE TABLE IF NOT EXISTS ai_agent_usage
(
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    source_scene VARCHAR(64),
    provider_name VARCHAR(128),
    model_code VARCHAR(128),
    prompt_tokens INTEGER NOT NULL DEFAULT 0,
    completion_tokens INTEGER NOT NULL DEFAULT 0,
    total_tokens INTEGER NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    usage_source VARCHAR(64) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_agent_usage_user_time
    ON ai_agent_usage(user_id, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_agent_usage_session_time
    ON ai_agent_usage(session_id, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_agent_usage_message
    ON ai_agent_usage(message_id);
