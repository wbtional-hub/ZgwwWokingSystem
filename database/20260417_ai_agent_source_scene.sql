ALTER TABLE ai_agent_session
  ADD COLUMN IF NOT EXISTS source_scene VARCHAR(64) NOT NULL DEFAULT 'AI_WORKBENCH';

COMMENT ON COLUMN ai_agent_session.source_scene IS 'Conversation source scene, such as AI_WORKBENCH or MOBILE_POLICY_CONSULTANT';

CREATE INDEX IF NOT EXISTS idx_ai_agent_session_source_scene
  ON ai_agent_session(source_scene, create_time DESC);
