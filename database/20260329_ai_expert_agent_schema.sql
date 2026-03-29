CREATE TABLE IF NOT EXISTS ai_skill_owner (
  id BIGSERIAL PRIMARY KEY,
  skill_id BIGINT NOT NULL,
  skill_version_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  expert_level VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  status SMALLINT NOT NULL DEFAULT 1,
  grant_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, skill_id)
);

COMMENT ON TABLE ai_skill_owner IS 'Skill-to-user expert ownership';
COMMENT ON COLUMN ai_skill_owner.skill_id IS 'Skill id';
COMMENT ON COLUMN ai_skill_owner.skill_version_id IS 'Skill version id';
COMMENT ON COLUMN ai_skill_owner.user_id IS 'Bound user id';
COMMENT ON COLUMN ai_skill_owner.expert_level IS 'Expert level';
COMMENT ON COLUMN ai_skill_owner.status IS 'Status: 1 enabled, 0 disabled';
COMMENT ON COLUMN ai_skill_owner.grant_time IS 'Grant time';

CREATE TABLE IF NOT EXISTS ai_agent_session (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  skill_id BIGINT NOT NULL,
  skill_version_id BIGINT NOT NULL,
  provider_config_id BIGINT,
  model_code VARCHAR(128),
  base_id BIGINT,
  session_title VARCHAR(255),
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_agent_session IS 'AI workbench conversation session';
COMMENT ON COLUMN ai_agent_session.user_id IS 'Question user id';
COMMENT ON COLUMN ai_agent_session.skill_id IS 'Selected skill id';
COMMENT ON COLUMN ai_agent_session.skill_version_id IS 'Selected skill version id';
COMMENT ON COLUMN ai_agent_session.provider_config_id IS 'Selected AI provider config id';
COMMENT ON COLUMN ai_agent_session.model_code IS 'Selected AI model code';
COMMENT ON COLUMN ai_agent_session.base_id IS 'Selected knowledge base id';
COMMENT ON COLUMN ai_agent_session.session_title IS 'Session title';
COMMENT ON COLUMN ai_agent_session.status IS 'Session status';

CREATE TABLE IF NOT EXISTS ai_agent_message (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL,
  message_role VARCHAR(32) NOT NULL,
  message_text TEXT NOT NULL,
  cited_chunk_ids TEXT,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_agent_message IS 'Conversation messages';
COMMENT ON COLUMN ai_agent_message.session_id IS 'Session id';
COMMENT ON COLUMN ai_agent_message.message_role IS 'Role: user/assistant/system';
COMMENT ON COLUMN ai_agent_message.message_text IS 'Message text';
COMMENT ON COLUMN ai_agent_message.cited_chunk_ids IS 'Referenced chunk ids';

CREATE INDEX IF NOT EXISTS idx_ai_skill_owner_user_skill
  ON ai_skill_owner(user_id, skill_id, status);

CREATE INDEX IF NOT EXISTS idx_ai_agent_session_user_time
  ON ai_agent_session(user_id, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_agent_session_skill
  ON ai_agent_session(skill_id, skill_version_id, status);

CREATE INDEX IF NOT EXISTS idx_ai_agent_session_provider_model
  ON ai_agent_session(provider_config_id, model_code, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_agent_message_session_time
  ON ai_agent_message(session_id, create_time ASC);
