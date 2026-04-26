BEGIN;

-- =========================================================
-- 1. 删除旧对象
-- =========================================================

DROP TABLE IF EXISTS ai_policy_answer_log CASCADE;
DROP TABLE IF EXISTS ai_policy_retrieval_log CASCADE;
DROP TABLE IF EXISTS ai_policy_eval_case CASCADE;
DROP TABLE IF EXISTS ai_policy_route_rule CASCADE;
DROP TABLE IF EXISTS ai_policy_faq CASCADE;
DROP TABLE IF EXISTS ai_policy_alias CASCADE;
DROP TABLE IF EXISTS ai_policy_chunk CASCADE;
DROP TABLE IF EXISTS ai_policy_document CASCADE;

DROP SEQUENCE IF EXISTS seq_ai_policy_answer_log CASCADE;
DROP SEQUENCE IF EXISTS seq_ai_policy_retrieval_log CASCADE;
DROP SEQUENCE IF EXISTS seq_ai_policy_eval_case CASCADE;
DROP SEQUENCE IF EXISTS seq_ai_policy_route_rule CASCADE;
DROP SEQUENCE IF EXISTS seq_ai_policy_faq CASCADE;
DROP SEQUENCE IF EXISTS seq_ai_policy_alias CASCADE;
DROP SEQUENCE IF EXISTS seq_ai_policy_chunk CASCADE;
DROP SEQUENCE IF EXISTS seq_ai_policy_document CASCADE;

-- =========================================================
-- 2. 重建表结构
-- =========================================================

CREATE TABLE ai_policy_document (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    region_scope VARCHAR(64) NOT NULL,
    policy_key VARCHAR(128),
    policy_name VARCHAR(255),
    doc_type VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_file_name VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_policy_chunk (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    region_scope VARCHAR(64) NOT NULL,
    policy_key VARCHAR(128),
    policy_name VARCHAR(255),
    policy_aliases TEXT,
    doc_type VARCHAR(64) NOT NULL,
    topic_type VARCHAR(64) NOT NULL,
    question_type VARCHAR(64) NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    answer_level VARCHAR(64) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    source_scene VARCHAR(64) NOT NULL,
    sort_no INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_policy_chunk_document
        FOREIGN KEY (document_id) REFERENCES ai_policy_document(id) ON DELETE CASCADE
);

CREATE TABLE ai_policy_faq (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    region_scope VARCHAR(64) NOT NULL,
    policy_key VARCHAR(128),
    policy_name VARCHAR(255),
    slot_code VARCHAR(128) NOT NULL,
    question_pattern VARCHAR(500) NOT NULL,
    standard_question VARCHAR(500) NOT NULL,
    standard_answer TEXT NOT NULL,
    evidence_source VARCHAR(500),
    priority INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_policy_alias (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    region_scope VARCHAR(64) NOT NULL,
    policy_key VARCHAR(128) NOT NULL,
    alias VARCHAR(255) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_policy_route_rule (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    region_scope VARCHAR(64),
    question_type VARCHAR(64) NOT NULL,
    keyword_pattern VARCHAR(500) NOT NULL,
    target_doc_type VARCHAR(64) NOT NULL,
    target_topic_type VARCHAR(64) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_policy_answer_log (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    session_id BIGINT,
    user_id BIGINT,
    source_scene VARCHAR(64) NOT NULL,
    applied_new_chain BOOLEAN NOT NULL DEFAULT TRUE,
    raw_question TEXT NOT NULL,
    normalized_question TEXT,
    region_scope VARCHAR(64),
    policy_key VARCHAR(128),
    question_type VARCHAR(64),
    route_plan TEXT,
    faq_hit BOOLEAN NOT NULL DEFAULT FALSE,
    fallback_flag BOOLEAN NOT NULL DEFAULT FALSE,
    hit_chunk_ids TEXT,
    answer_mode VARCHAR(64),
    validation_summary TEXT,
    final_answer TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_policy_retrieval_log (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    session_id BIGINT,
    user_id BIGINT,
    source_scene VARCHAR(64) NOT NULL,
    raw_question TEXT NOT NULL,
    normalized_question TEXT,
    region_scope VARCHAR(64),
    policy_key VARCHAR(128),
    question_type VARCHAR(64),
    route_plan TEXT,
    search_query TEXT,
    hit_chunk_ids TEXT,
    retrieval_summary TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_policy_eval_case (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    region_scope VARCHAR(64),
    question VARCHAR(500) NOT NULL,
    expected_policy_key VARCHAR(128),
    expected_question_type VARCHAR(64),
    expected_answer_contains VARCHAR(500),
    expected_forbidden_keywords VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- 3. 索引
-- =========================================================

CREATE INDEX idx_ai_policy_document_base_region
    ON ai_policy_document(base_id, region_scope, doc_type, enabled);

CREATE INDEX idx_ai_policy_chunk_base_region
    ON ai_policy_chunk(base_id, region_scope, doc_type, topic_type, enabled);

CREATE INDEX idx_ai_policy_chunk_policy_key
    ON ai_policy_chunk(base_id, policy_key, question_type, priority);

CREATE INDEX idx_ai_policy_chunk_document_id
    ON ai_policy_chunk(document_id);

CREATE INDEX idx_ai_policy_faq_base_region
    ON ai_policy_faq(base_id, region_scope, policy_key, priority);

CREATE UNIQUE INDEX uk_ai_policy_alias_base_key_alias
    ON ai_policy_alias(base_id, policy_key, alias);

CREATE INDEX idx_ai_policy_route_rule_base
    ON ai_policy_route_rule(base_id, question_type, region_scope, priority);

CREATE INDEX idx_ai_policy_answer_log_base_time
    ON ai_policy_answer_log(base_id, created_at DESC);

CREATE INDEX idx_ai_policy_retrieval_log_base_time
    ON ai_policy_retrieval_log(base_id, created_at DESC);

CREATE INDEX idx_ai_policy_eval_case_base
    ON ai_policy_eval_case(base_id, enabled, region_scope);

-- 可选：模糊匹配增强
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_ai_policy_alias_alias_trgm
    ON ai_policy_alias USING gin (alias gin_trgm_ops);

CREATE INDEX idx_ai_policy_faq_question_trgm
    ON ai_policy_faq USING gin (standard_question gin_trgm_ops);

CREATE INDEX idx_ai_policy_faq_pattern_trgm
    ON ai_policy_faq USING gin (question_pattern gin_trgm_ops);

-- =========================================================
-- 4. 初始化 alias
-- 默认 base_id = 1
-- =========================================================

INSERT INTO ai_policy_alias
(base_id, region_scope, policy_key, alias, priority, enabled, created_at, updated_at)
VALUES
(1, 'xiamen_city', 'double_hundred', '双百计划', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'double_hundred', '双百人才', 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'double_hundred', '厦门双百', 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'special_post', '特聘岗位', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'special_post', '高层次人才特聘岗位', 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'special_fund', '专项资金', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'special_fund', '创业扶持资金', 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'housing', '住房', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'housing', '安居', 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'housing', '住房补贴', 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'ai_talent', 'AI', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'ai_talent', '人工智能', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'ai_talent', 'AI人才专项', 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'postdoc', '博士后', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'postdoc', '工作站', 80, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'service_support', '服务保障', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'service_support', '子女教育', 95, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'service_support', '医疗保障', 95, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'service_support', '平台申报', 85, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'fujian_province', 'fujian_bairen', '福建省百人计划', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'fujian_province', 'fujian_bairen', '引才百人计划', 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'fujian_province', 'fujian_bairen', '创业创新人才项目', 80, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =========================================================
-- 5. 初始化 route_rule
-- =========================================================

INSERT INTO ai_policy_route_rule
(base_id, region_scope, question_type, keyword_pattern, target_doc_type, target_topic_type, priority, enabled, created_at, updated_at)
VALUES
(1, 'xiamen_city', 'process', '双百计划|创新团队|创业人才', 'topic', 'process', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'condition', '双百计划|特聘岗位|专项资金', 'topic', 'condition', 95, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'benefit', '双百计划|专项资金|住房|AI|博士后', 'topic', 'benefit', 95, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'service', '服务保障|子女教育|医疗保障|平台申报', 'topic', 'service', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'fujian_province', 'condition', '高层次人才认定|百人计划|近期申报|专项支持', 'topic', 'condition', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, NULL, 'list', '人才政策|项目清单|有哪些', 'main', 'overview', 80, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =========================================================
-- 6. 初始化 FAQ
-- =========================================================

INSERT INTO ai_policy_faq
(base_id, region_scope, policy_key, policy_name, slot_code, question_pattern, standard_question, standard_answer, evidence_source, priority, enabled, created_at, updated_at)
VALUES
(1, 'xiamen_city', 'double_hundred', '厦门市引进高层次创新创业人才“双百计划”实施意见', 'overview', '双百计划|是什么', '双百计划是什么', '双百计划属于厦门市引进高层次创新创业人才的重要专题政策，具体适用对象、申报流程和支持标准需结合对应专题条款进一步确认。', '双百计划专题（问答增强版）', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'double_hundred', '厦门市引进高层次创新创业人才“双百计划”实施意见', 'target', '双百计划|支持哪些对象', '双百计划支持哪些对象', '双百计划重点围绕高层次创新创业人才展开，需结合创新个人、创新团队、创业人才等具体申报类别分别判断。', '双百计划专题（问答增强版）', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'double_hundred', '厦门市引进高层次创新创业人才“双百计划”实施意见', 'process', '双百计划|怎么申请|申请流程|申报流程', '双百计划怎么申请', '双百计划申请前需先区分申报类别。当前库内已确认创新团队、创业人才一般按组织申报、资格核查、部门联审、综合评审、研究确认等步骤推进，具体材料和时间节点仍以当年公告为准。', '双百计划专题（问答增强版）', 120, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'double_hundred', '厦门市引进高层次创新创业人才“双百计划”实施意见', 'benefit', '双百计划|补助多少', '双百计划补助多少', '双百计划补助标准需结合具体类别和条款确认，建议继续命中双百计划待遇/支持标准专题块。', '双百计划专题（问答增强版）', 95, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'double_hundred', '厦门市引进高层次创新创业人才“双百计划”实施意见', 'period', '双百计划|管理期多久', '双百计划管理期多久', '双百计划管理期应以具体项目管理条款为准，建议继续命中管理期专题块确认。', '双百计划专题（问答增强版）', 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'special_post', '厦门市高层次人才特聘岗位实施方案', 'overview', '特聘岗位|是什么', '特聘岗位是什么', '特聘岗位属于厦门市高层次人才专题政策，需要结合对象、岗位条件和申报流程专题进一步确认。', '特聘岗位专题（问答增强版）', 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'special_post', '厦门市高层次人才特聘岗位实施方案', 'process', '特聘岗位|怎么申报|申报流程', '特聘岗位怎么申报', '特聘岗位申报应优先查看特聘岗位专题流程块，按岗位发布、申报审核和确认程序办理，具体材料与时间以年度通知为准。', '特聘岗位专题（问答增强版）', 95, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'special_fund', '厦门市高层次人才专项资金管理办法', 'target', '专项资金|支持谁', '专项资金支持谁', '专项资金支持对象需按专项资金管理办法和对应专题条款确认，不能泛化替代其他产业项目。', '专项资金专题（问答增强版）', 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'special_fund', '厦门市高层次人才专项资金管理办法', 'payment', '创业资金|怎么拨|拨付', '创业资金怎么拨', '创业资金拨付应优先命中专项资金拨付/兑现专题条款，若涉及分阶段拨付，以实际条款和年度要求为准。', '专项资金专题（问答增强版）', 92, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', NULL, NULL, 'list', '厦门|有哪些人才政策', '厦门有哪些人才政策', '厦门人才政策应优先按统领政策、重点产业项目、住房、博士后、AI、服务保障等专题目录回答。', '厦门市人才政策（清洗导入版 v2）', 88, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'ai_talent', '厦门市支持人工智能领域人才发展的若干措施', 'ai', '厦门|AI|人工智能|人才专项', '厦门有 AI 人才专项吗', '厦门 AI 人才问题应优先命中人工智能专题政策，不能先回无关产业项目。', 'AI 专题', 96, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'housing', '厦门市引进高层次人才住房补贴实施意见', 'housing', '住房|安居|住房补贴|有哪些', '住房类人才政策有哪些', '住房类问题应优先命中住房专题，不能先回产业补助。', '住房专题', 98, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'service_support', '服务保障专题', 'service', '服务保障|子女教育|医疗保障|平台申报', '人才服务保障包括什么', '服务保障问题应优先命中服务保障专题，围绕子女教育、医疗保障、平台申报等专题条款回答。', '服务保障专题', 98, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'fujian_province', 'fujian_high_level', '福建省高层次人才认定与支持专题', 'condition', '福建省|高层次人才|如何认定', '福建省高层次人才如何认定', '福建省高层次人才认定应优先命中认定与支持专题，围绕认定条件、程序和支持条款回答。', '福建省高层次人才认定与支持专题', 98, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'fujian_province', 'fujian_recent_apply', '福建省近期申报与专项支持专题', 'list', '福建省|近期|申报项目', '福建省近期有哪些申报项目', '福建省近期申报项目应优先命中近期申报与专项支持专题，结合公告和专项支持条款回答。', '福建省近期申报与专项支持专题', 95, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'fujian_province', 'fujian_bairen', '福建省引才“百人计划”专题', 'benefit', '福建省|百人计划|补助多少', '福建省百人计划补助多少', '福建省百人计划补助问题应优先命中百人计划专题待遇条款，避免混用厦门市级政策。', '福建省引才“百人计划”专题', 95, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =========================================================
-- 7. 初始化评测题
-- =========================================================

INSERT INTO ai_policy_eval_case
(base_id, region_scope, question, expected_policy_key, expected_question_type, expected_answer_contains, expected_forbidden_keywords, enabled, created_at)
VALUES
(1, 'xiamen_city', '双百计划是什么', 'double_hundred', 'topic', '双百计划', NULL, TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '双百计划支持哪些对象', 'double_hundred', 'condition', '创新', NULL, TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '双百计划怎么申请', 'double_hundred', 'process', '组织申报', '无法确认', TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '双百计划补助多少', 'double_hundred', 'benefit', '双百计划', NULL, TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '双百计划管理期多久', 'double_hundred', 'risk', '管理期', NULL, TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '创业人才可以提现吗分阶段提现吗', 'double_hundred', 'benefit', '拨付', NULL, TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '特聘岗位是什么', 'special_post', 'topic', '特聘岗位', NULL, TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '特聘岗位怎么申报', 'special_post', 'process', '申报', NULL, TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '专项资金支持谁', 'special_fund', 'condition', '专项资金', NULL, TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '创业资金怎么拨', 'special_fund', 'benefit', '拨付', NULL, TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '厦门有哪些人才政策', NULL, 'list', '厦门', '福建省', TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '厦门有 AI 人才专项吗', 'ai_talent', 'topic', 'AI', '产业人才项目', TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '住房类人才政策有哪些', 'housing', 'list', '住房', '产业补助', TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '人才服务保障包括什么', 'service_support', 'service', '服务保障', '产业项目', TRUE, CURRENT_TIMESTAMP),
(1, 'fujian_province', '福建省高层次人才如何认定', 'fujian_high_level', 'condition', '福建省', '厦门市', TRUE, CURRENT_TIMESTAMP),
(1, 'fujian_province', '福建省近期有哪些申报项目', 'fujian_recent_apply', 'list', '福建省', '厦门市', TRUE, CURRENT_TIMESTAMP),
(1, 'fujian_province', '福建省百人计划补助多少', 'fujian_bairen', 'benefit', '百人计划', '厦门市', TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '厦门问题不能被福建省答案替代', NULL, 'topic', '厦门', '福建省', TRUE, CURRENT_TIMESTAMP),
(1, 'fujian_province', '福建省问题不能被厦门答案替代', NULL, 'topic', '福建省', '厦门市', TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '住房问题不能先回产业补助', 'housing', 'topic', '住房', '产业补助', TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', 'AI 问题不能先回无关产业项目', 'ai_talent', 'topic', 'AI', '产业项目', TRUE, CURRENT_TIMESTAMP),
(1, 'xiamen_city', '双百细节问题不能只靠主文档首答', 'double_hundred', 'process', '继续命中专题', '仅主文档', TRUE, CURRENT_TIMESTAMP);

COMMIT;