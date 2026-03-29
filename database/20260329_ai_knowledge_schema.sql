CREATE TABLE IF NOT EXISTS ai_knowledge_base (
  id BIGSERIAL PRIMARY KEY,
  base_code VARCHAR(64) NOT NULL UNIQUE,
  base_name VARCHAR(128) NOT NULL,
  domain_type VARCHAR(64),
  description TEXT,
  status SMALLINT NOT NULL DEFAULT 1,
  owner_user_id BIGINT,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_user VARCHAR(64) NOT NULL DEFAULT 'system',
  update_user VARCHAR(64) NOT NULL DEFAULT 'system',
  is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE ai_knowledge_base IS 'AI knowledge base master table';
COMMENT ON COLUMN ai_knowledge_base.base_code IS 'Knowledge base code';
COMMENT ON COLUMN ai_knowledge_base.base_name IS 'Knowledge base name';
COMMENT ON COLUMN ai_knowledge_base.domain_type IS 'Domain type';
COMMENT ON COLUMN ai_knowledge_base.description IS 'Knowledge base description';
COMMENT ON COLUMN ai_knowledge_base.status IS 'Status: 1 enabled, 0 disabled';
COMMENT ON COLUMN ai_knowledge_base.owner_user_id IS 'Owner user id';

CREATE TABLE IF NOT EXISTS ai_knowledge_category (
  id BIGSERIAL PRIMARY KEY,
  base_id BIGINT NOT NULL,
  parent_id BIGINT,
  category_code VARCHAR(64) NOT NULL,
  category_name VARCHAR(128) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status SMALLINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE ai_knowledge_category IS 'AI knowledge base category tree';
COMMENT ON COLUMN ai_knowledge_category.base_id IS 'Knowledge base id';
COMMENT ON COLUMN ai_knowledge_category.parent_id IS 'Parent category id';
COMMENT ON COLUMN ai_knowledge_category.category_code IS 'Category code';
COMMENT ON COLUMN ai_knowledge_category.category_name IS 'Category name';
COMMENT ON COLUMN ai_knowledge_category.sort_no IS 'Sort number';

CREATE TABLE IF NOT EXISTS ai_knowledge_document (
  id BIGSERIAL PRIMARY KEY,
  base_id BIGINT NOT NULL,
  category_id BIGINT,
  doc_title VARCHAR(255) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  file_name VARCHAR(255),
  file_path VARCHAR(500),
  doc_type VARCHAR(32) NOT NULL,
  policy_region VARCHAR(128),
  policy_level VARCHAR(64),
  effective_date DATE,
  expire_date DATE,
  keywords TEXT,
  summary TEXT,
  parse_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  status SMALLINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_user VARCHAR(64) NOT NULL DEFAULT 'system',
  update_user VARCHAR(64) NOT NULL DEFAULT 'system',
  is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE ai_knowledge_document IS 'Knowledge documents imported into the base';
COMMENT ON COLUMN ai_knowledge_document.base_id IS 'Knowledge base id';
COMMENT ON COLUMN ai_knowledge_document.category_id IS 'Knowledge category id';
COMMENT ON COLUMN ai_knowledge_document.doc_title IS 'Document title';
COMMENT ON COLUMN ai_knowledge_document.source_type IS 'Source type: upload/manual/import';
COMMENT ON COLUMN ai_knowledge_document.file_name IS 'Original file name';
COMMENT ON COLUMN ai_knowledge_document.file_path IS 'Stored file path';
COMMENT ON COLUMN ai_knowledge_document.doc_type IS 'Document type such as policy/guide/qa';
COMMENT ON COLUMN ai_knowledge_document.policy_region IS 'Applicable region';
COMMENT ON COLUMN ai_knowledge_document.policy_level IS 'Policy level';
COMMENT ON COLUMN ai_knowledge_document.effective_date IS 'Effective date';
COMMENT ON COLUMN ai_knowledge_document.expire_date IS 'Expire date';
COMMENT ON COLUMN ai_knowledge_document.keywords IS 'Keywords';
COMMENT ON COLUMN ai_knowledge_document.summary IS 'Summary';
COMMENT ON COLUMN ai_knowledge_document.parse_status IS 'Parse status';

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
  id BIGSERIAL PRIMARY KEY,
  document_id BIGINT NOT NULL,
  base_id BIGINT NOT NULL,
  category_id BIGINT,
  chunk_no INT NOT NULL,
  chunk_type VARCHAR(32) NOT NULL,
  heading_path VARCHAR(500),
  content_text TEXT NOT NULL,
  keyword_text TEXT,
  content_length INT,
  sort_no INT NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_knowledge_chunk IS 'Document chunk data for search and retrieval';
COMMENT ON COLUMN ai_knowledge_chunk.document_id IS 'Document id';
COMMENT ON COLUMN ai_knowledge_chunk.base_id IS 'Knowledge base id';
COMMENT ON COLUMN ai_knowledge_chunk.category_id IS 'Knowledge category id';
COMMENT ON COLUMN ai_knowledge_chunk.chunk_no IS 'Chunk sequence number';
COMMENT ON COLUMN ai_knowledge_chunk.chunk_type IS 'Chunk type: paragraph/table/summary';
COMMENT ON COLUMN ai_knowledge_chunk.heading_path IS 'Heading path';
COMMENT ON COLUMN ai_knowledge_chunk.content_text IS 'Chunk content';
COMMENT ON COLUMN ai_knowledge_chunk.keyword_text IS 'Chunk keywords';
COMMENT ON COLUMN ai_knowledge_chunk.content_length IS 'Content length';

CREATE TABLE IF NOT EXISTS ai_document_import_job (
  id BIGSERIAL PRIMARY KEY,
  base_id BIGINT NOT NULL,
  category_id BIGINT,
  file_name VARCHAR(255) NOT NULL,
  job_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  total_chunks INT NOT NULL DEFAULT 0,
  success_chunks INT NOT NULL DEFAULT 0,
  error_message TEXT,
  start_time TIMESTAMP,
  finish_time TIMESTAMP,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_user VARCHAR(64) NOT NULL DEFAULT 'system'
);

COMMENT ON TABLE ai_document_import_job IS 'Document import job records';
COMMENT ON COLUMN ai_document_import_job.base_id IS 'Knowledge base id';
COMMENT ON COLUMN ai_document_import_job.category_id IS 'Knowledge category id';
COMMENT ON COLUMN ai_document_import_job.file_name IS 'Import file name';
COMMENT ON COLUMN ai_document_import_job.job_status IS 'Job status';
COMMENT ON COLUMN ai_document_import_job.total_chunks IS 'Total parsed chunks';
COMMENT ON COLUMN ai_document_import_job.success_chunks IS 'Successfully inserted chunks';
COMMENT ON COLUMN ai_document_import_job.error_message IS 'Error message';

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_knowledge_category_base_code
  ON ai_knowledge_category(base_id, category_code)
  WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_knowledge_base_status
  ON ai_knowledge_base(status, update_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_knowledge_category_base_parent
  ON ai_knowledge_category(base_id, parent_id, sort_no);

CREATE INDEX IF NOT EXISTS idx_ai_kb_doc_base_cat_status
  ON ai_knowledge_document(base_id, category_id, status, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_kb_doc_region_date
  ON ai_knowledge_document(policy_region, effective_date, expire_date);

CREATE INDEX IF NOT EXISTS idx_ai_kb_doc_parse_status
  ON ai_knowledge_document(parse_status, update_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_kb_chunk_base_cat_doc
  ON ai_knowledge_chunk(base_id, category_id, document_id, sort_no);

CREATE INDEX IF NOT EXISTS idx_ai_kb_chunk_document_chunkno
  ON ai_knowledge_chunk(document_id, chunk_no);

CREATE INDEX IF NOT EXISTS idx_ai_import_job_base_status
  ON ai_document_import_job(base_id, job_status, create_time DESC);
