ALTER TABLE ai_agent_session
  ADD COLUMN IF NOT EXISTS source_scene VARCHAR(64) NOT NULL DEFAULT 'AI_WORKBENCH';

CREATE INDEX IF NOT EXISTS idx_ai_agent_session_source_scene
  ON ai_agent_session(source_scene, create_time DESC);
