ALTER TABLE ai_knowledge_base
  ADD COLUMN IF NOT EXISTS remark TEXT;

COMMENT ON COLUMN ai_knowledge_base.remark IS 'Maintenance remark for knowledge base';
