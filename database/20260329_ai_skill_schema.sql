CREATE TABLE IF NOT EXISTS ai_skill (
  id BIGSERIAL PRIMARY KEY,
  skill_code VARCHAR(64) NOT NULL UNIQUE,
  skill_name VARCHAR(128) NOT NULL,
  domain_type VARCHAR(64),
  skill_type VARCHAR(64),
  description TEXT,
  status SMALLINT NOT NULL DEFAULT 1,
  owner_user_id BIGINT,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_user VARCHAR(64) NOT NULL DEFAULT 'system',
  update_user VARCHAR(64) NOT NULL DEFAULT 'system',
  is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE ai_skill IS 'AI skill definition';
COMMENT ON COLUMN ai_skill.skill_code IS 'Skill code';
COMMENT ON COLUMN ai_skill.skill_name IS 'Skill name';
COMMENT ON COLUMN ai_skill.domain_type IS 'Domain type';
COMMENT ON COLUMN ai_skill.skill_type IS 'Skill type';
COMMENT ON COLUMN ai_skill.description IS 'Skill description';
COMMENT ON COLUMN ai_skill.status IS 'Status: 1 enabled, 0 disabled';
COMMENT ON COLUMN ai_skill.owner_user_id IS 'Skill owner user id';

CREATE TABLE IF NOT EXISTS ai_skill_version (
  id BIGSERIAL PRIMARY KEY,
  skill_id BIGINT NOT NULL,
  version_no VARCHAR(32) NOT NULL,
  provider_config_id BIGINT,
  model_code VARCHAR(128),
  system_prompt TEXT NOT NULL,
  task_prompt TEXT,
  output_template TEXT,
  forbidden_rules TEXT,
  citation_rules TEXT,
  validation_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  publish_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  score NUMERIC(5,2),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_user VARCHAR(64) NOT NULL DEFAULT 'system',
  UNIQUE (skill_id, version_no)
);

COMMENT ON TABLE ai_skill_version IS 'Skill version definitions';
COMMENT ON COLUMN ai_skill_version.skill_id IS 'Skill id';
COMMENT ON COLUMN ai_skill_version.version_no IS 'Version number';
COMMENT ON COLUMN ai_skill_version.provider_config_id IS 'Default AI provider config id';
COMMENT ON COLUMN ai_skill_version.model_code IS 'Default model code';
COMMENT ON COLUMN ai_skill_version.system_prompt IS 'System prompt';
COMMENT ON COLUMN ai_skill_version.task_prompt IS 'Task prompt';
COMMENT ON COLUMN ai_skill_version.output_template IS 'Output template';
COMMENT ON COLUMN ai_skill_version.forbidden_rules IS 'Forbidden rules';
COMMENT ON COLUMN ai_skill_version.citation_rules IS 'Citation rules';
COMMENT ON COLUMN ai_skill_version.validation_status IS 'Validation status';
COMMENT ON COLUMN ai_skill_version.publish_status IS 'Publish status';
COMMENT ON COLUMN ai_skill_version.score IS 'Validation score';

CREATE TABLE IF NOT EXISTS ai_skill_kb_binding (
  id BIGSERIAL PRIMARY KEY,
  skill_id BIGINT NOT NULL,
  skill_version_id BIGINT NOT NULL,
  base_id BIGINT NOT NULL,
  category_id BIGINT,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_skill_kb_binding IS 'Binding between skill versions and knowledge bases';
COMMENT ON COLUMN ai_skill_kb_binding.skill_id IS 'Skill id';
COMMENT ON COLUMN ai_skill_kb_binding.skill_version_id IS 'Skill version id';
COMMENT ON COLUMN ai_skill_kb_binding.base_id IS 'Knowledge base id';
COMMENT ON COLUMN ai_skill_kb_binding.category_id IS 'Knowledge category id';

CREATE TABLE IF NOT EXISTS ai_skill_test_case (
  id BIGSERIAL PRIMARY KEY,
  skill_id BIGINT NOT NULL,
  skill_version_id BIGINT NOT NULL,
  case_type VARCHAR(32) NOT NULL,
  question_text TEXT NOT NULL,
  expected_points TEXT,
  expected_format TEXT,
  standard_answer TEXT,
  status SMALLINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_skill_test_case IS 'Skill validation test cases';
COMMENT ON COLUMN ai_skill_test_case.skill_id IS 'Skill id';
COMMENT ON COLUMN ai_skill_test_case.skill_version_id IS 'Skill version id';
COMMENT ON COLUMN ai_skill_test_case.case_type IS 'Case type';
COMMENT ON COLUMN ai_skill_test_case.question_text IS 'Validation question';
COMMENT ON COLUMN ai_skill_test_case.expected_points IS 'Expected points';
COMMENT ON COLUMN ai_skill_test_case.expected_format IS 'Expected format';
COMMENT ON COLUMN ai_skill_test_case.standard_answer IS 'Reference answer';

CREATE TABLE IF NOT EXISTS ai_skill_validation_run (
  id BIGSERIAL PRIMARY KEY,
  skill_id BIGINT NOT NULL,
  skill_version_id BIGINT NOT NULL,
  provider_config_id BIGINT,
  model_code VARCHAR(128),
  run_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  pass_rate NUMERIC(5,2),
  citation_rate NUMERIC(5,2),
  avg_score NUMERIC(5,2),
  start_time TIMESTAMP,
  finish_time TIMESTAMP,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_skill_validation_run IS 'Skill validation run summary';
COMMENT ON COLUMN ai_skill_validation_run.skill_id IS 'Skill id';
COMMENT ON COLUMN ai_skill_validation_run.skill_version_id IS 'Skill version id';
COMMENT ON COLUMN ai_skill_validation_run.provider_config_id IS 'Validation provider config id';
COMMENT ON COLUMN ai_skill_validation_run.model_code IS 'Validation model code';
COMMENT ON COLUMN ai_skill_validation_run.run_status IS 'Run status';
COMMENT ON COLUMN ai_skill_validation_run.pass_rate IS 'Pass rate';
COMMENT ON COLUMN ai_skill_validation_run.citation_rate IS 'Citation rate';
COMMENT ON COLUMN ai_skill_validation_run.avg_score IS 'Average score';

CREATE TABLE IF NOT EXISTS ai_skill_validation_result (
  id BIGSERIAL PRIMARY KEY,
  run_id BIGINT NOT NULL,
  test_case_id BIGINT NOT NULL,
  answer_text TEXT,
  hit_chunks TEXT,
  score NUMERIC(5,2),
  is_pass BOOLEAN NOT NULL DEFAULT FALSE,
  fail_reason TEXT
);

COMMENT ON TABLE ai_skill_validation_result IS 'Skill validation run details';
COMMENT ON COLUMN ai_skill_validation_result.run_id IS 'Validation run id';
COMMENT ON COLUMN ai_skill_validation_result.test_case_id IS 'Test case id';
COMMENT ON COLUMN ai_skill_validation_result.answer_text IS 'Generated answer';
COMMENT ON COLUMN ai_skill_validation_result.hit_chunks IS 'Hit chunk ids';
COMMENT ON COLUMN ai_skill_validation_result.score IS 'Case score';
COMMENT ON COLUMN ai_skill_validation_result.is_pass IS 'Whether the case passed';
COMMENT ON COLUMN ai_skill_validation_result.fail_reason IS 'Failure reason';

CREATE INDEX IF NOT EXISTS idx_ai_skill_status
  ON ai_skill(status, update_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_skill_version_skill_publish
  ON ai_skill_version(skill_id, publish_status, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_skill_version_provider_model
  ON ai_skill_version(provider_config_id, model_code);

CREATE INDEX IF NOT EXISTS idx_ai_skill_binding_skill_version
  ON ai_skill_kb_binding(skill_id, skill_version_id);

CREATE INDEX IF NOT EXISTS idx_ai_skill_binding_base_category
  ON ai_skill_kb_binding(base_id, category_id);

CREATE INDEX IF NOT EXISTS idx_ai_skill_case_skill_version
  ON ai_skill_test_case(skill_id, skill_version_id, status);

CREATE INDEX IF NOT EXISTS idx_ai_skill_run_skill_version
  ON ai_skill_validation_run(skill_id, skill_version_id, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_skill_run_provider_model
  ON ai_skill_validation_run(provider_config_id, model_code, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_skill_result_run
  ON ai_skill_validation_result(run_id, test_case_id);
