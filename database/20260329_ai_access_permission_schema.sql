CREATE TABLE IF NOT EXISTS ai_provider_config (
  id BIGSERIAL PRIMARY KEY,
  provider_code VARCHAR(64) NOT NULL UNIQUE,
  provider_name VARCHAR(128) NOT NULL,
  api_base_url VARCHAR(500),
  api_token_cipher TEXT NOT NULL,
  token_mask VARCHAR(64),
  default_model VARCHAR(128),
  connect_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  status SMALLINT NOT NULL DEFAULT 1,
  remark TEXT,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_user VARCHAR(64) NOT NULL DEFAULT 'system',
  update_user VARCHAR(64) NOT NULL DEFAULT 'system',
  is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE ai_provider_config IS 'AI provider connection settings';
COMMENT ON COLUMN ai_provider_config.provider_code IS 'Provider code';
COMMENT ON COLUMN ai_provider_config.provider_name IS 'Provider display name';
COMMENT ON COLUMN ai_provider_config.api_base_url IS 'Provider API base url';
COMMENT ON COLUMN ai_provider_config.api_token_cipher IS 'Encrypted API token';
COMMENT ON COLUMN ai_provider_config.token_mask IS 'Masked token for UI';
COMMENT ON COLUMN ai_provider_config.default_model IS 'Default model code';
COMMENT ON COLUMN ai_provider_config.connect_status IS 'Connection test status';
COMMENT ON COLUMN ai_provider_config.status IS 'Status: 1 enabled, 0 disabled';

CREATE TABLE IF NOT EXISTS ai_provider_model (
  id BIGSERIAL PRIMARY KEY,
  provider_config_id BIGINT NOT NULL,
  model_code VARCHAR(128) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  model_type VARCHAR(64),
  support_knowledge BOOLEAN NOT NULL DEFAULT TRUE,
  support_skill_train BOOLEAN NOT NULL DEFAULT TRUE,
  support_agent_chat BOOLEAN NOT NULL DEFAULT TRUE,
  support_analysis BOOLEAN NOT NULL DEFAULT TRUE,
  status SMALLINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (provider_config_id, model_code)
);

COMMENT ON TABLE ai_provider_model IS 'Models exposed by an AI provider';
COMMENT ON COLUMN ai_provider_model.provider_config_id IS 'Provider config id';
COMMENT ON COLUMN ai_provider_model.model_code IS 'Model code';
COMMENT ON COLUMN ai_provider_model.model_name IS 'Model display name';
COMMENT ON COLUMN ai_provider_model.model_type IS 'Model type';

CREATE TABLE IF NOT EXISTS ai_user_ai_permission (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  provider_config_id BIGINT NOT NULL,
  can_manage_provider BOOLEAN NOT NULL DEFAULT FALSE,
  can_use_ai BOOLEAN NOT NULL DEFAULT FALSE,
  can_train_skill BOOLEAN NOT NULL DEFAULT FALSE,
  can_publish_skill BOOLEAN NOT NULL DEFAULT FALSE,
  can_use_agent BOOLEAN NOT NULL DEFAULT FALSE,
  can_run_analysis BOOLEAN NOT NULL DEFAULT FALSE,
  status SMALLINT NOT NULL DEFAULT 1,
  grant_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  grant_user VARCHAR(64) NOT NULL DEFAULT 'system',
  UNIQUE (user_id, provider_config_id)
);

COMMENT ON TABLE ai_user_ai_permission IS 'User-level AI capability permission';
COMMENT ON COLUMN ai_user_ai_permission.user_id IS 'User id';
COMMENT ON COLUMN ai_user_ai_permission.provider_config_id IS 'Provider config id';
COMMENT ON COLUMN ai_user_ai_permission.can_manage_provider IS 'Can manage AI access settings';
COMMENT ON COLUMN ai_user_ai_permission.can_use_ai IS 'Can use AI capabilities';
COMMENT ON COLUMN ai_user_ai_permission.can_train_skill IS 'Can train skills';
COMMENT ON COLUMN ai_user_ai_permission.can_publish_skill IS 'Can publish skills';
COMMENT ON COLUMN ai_user_ai_permission.can_use_agent IS 'Can use AI workbench';
COMMENT ON COLUMN ai_user_ai_permission.can_run_analysis IS 'Can run AI analysis';

CREATE TABLE IF NOT EXISTS ai_user_knowledge_permission (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  base_id BIGINT NOT NULL,
  can_view BOOLEAN NOT NULL DEFAULT FALSE,
  can_upload BOOLEAN NOT NULL DEFAULT FALSE,
  can_train_skill BOOLEAN NOT NULL DEFAULT FALSE,
  can_analyze BOOLEAN NOT NULL DEFAULT FALSE,
  status SMALLINT NOT NULL DEFAULT 1,
  grant_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  grant_user VARCHAR(64) NOT NULL DEFAULT 'system',
  UNIQUE (user_id, base_id)
);

COMMENT ON TABLE ai_user_knowledge_permission IS 'User permission on a knowledge base';
COMMENT ON COLUMN ai_user_knowledge_permission.user_id IS 'User id';
COMMENT ON COLUMN ai_user_knowledge_permission.base_id IS 'Knowledge base id';
COMMENT ON COLUMN ai_user_knowledge_permission.can_view IS 'Can view knowledge base';
COMMENT ON COLUMN ai_user_knowledge_permission.can_upload IS 'Can upload documents';
COMMENT ON COLUMN ai_user_knowledge_permission.can_train_skill IS 'Can train skills using this base';
COMMENT ON COLUMN ai_user_knowledge_permission.can_analyze IS 'Can analyze with this base';

CREATE TABLE IF NOT EXISTS ai_user_skill_permission (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  skill_id BIGINT NOT NULL,
  can_view BOOLEAN NOT NULL DEFAULT FALSE,
  can_use BOOLEAN NOT NULL DEFAULT FALSE,
  can_train BOOLEAN NOT NULL DEFAULT FALSE,
  can_publish BOOLEAN NOT NULL DEFAULT FALSE,
  status SMALLINT NOT NULL DEFAULT 1,
  grant_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  grant_user VARCHAR(64) NOT NULL DEFAULT 'system',
  UNIQUE (user_id, skill_id)
);

COMMENT ON TABLE ai_user_skill_permission IS 'User permission on a skill';
COMMENT ON COLUMN ai_user_skill_permission.user_id IS 'User id';
COMMENT ON COLUMN ai_user_skill_permission.skill_id IS 'Skill id';
COMMENT ON COLUMN ai_user_skill_permission.can_view IS 'Can view skill';
COMMENT ON COLUMN ai_user_skill_permission.can_use IS 'Can use skill';
COMMENT ON COLUMN ai_user_skill_permission.can_train IS 'Can train skill';
COMMENT ON COLUMN ai_user_skill_permission.can_publish IS 'Can publish skill';

CREATE INDEX IF NOT EXISTS idx_ai_provider_config_status
  ON ai_provider_config(status, connect_status, update_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_provider_model_provider_status
  ON ai_provider_model(provider_config_id, status, model_code);

CREATE INDEX IF NOT EXISTS idx_ai_user_ai_permission_user_provider
  ON ai_user_ai_permission(user_id, provider_config_id, status);

CREATE INDEX IF NOT EXISTS idx_ai_user_knowledge_permission_user_base
  ON ai_user_knowledge_permission(user_id, base_id, status);

CREATE INDEX IF NOT EXISTS idx_ai_user_skill_permission_user_skill
  ON ai_user_skill_permission(user_id, skill_id, status);
