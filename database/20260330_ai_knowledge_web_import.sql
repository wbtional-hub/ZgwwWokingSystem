ALTER TABLE ai_knowledge_document
  ADD COLUMN IF NOT EXISTS source_url VARCHAR(500);

ALTER TABLE ai_knowledge_document
  ADD COLUMN IF NOT EXISTS fetch_time TIMESTAMP;

COMMENT ON COLUMN ai_knowledge_document.source_url IS 'Source URL for imported web content';
COMMENT ON COLUMN ai_knowledge_document.fetch_time IS 'Fetch time for imported web content';
