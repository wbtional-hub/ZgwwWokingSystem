CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS ai_policy_intent (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    region_scope VARCHAR(64),
    policy_key VARCHAR(128),
    intent_code VARCHAR(255) NOT NULL,
    intent_name VARCHAR(255) NOT NULL,
    standard_question VARCHAR(500) NOT NULL,
    question_type VARCHAR(64) NOT NULL,
    topic_type VARCHAR(64),
    answer_level_default VARCHAR(64) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_policy_intent_base_code
    ON ai_policy_intent(base_id, intent_code);
CREATE INDEX IF NOT EXISTS idx_ai_policy_intent_base_policy
    ON ai_policy_intent(base_id, region_scope, policy_key, question_type, enabled, priority DESC);

CREATE TABLE IF NOT EXISTS ai_policy_intent_phrase (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    intent_id BIGINT NOT NULL,
    phrase VARCHAR(500) NOT NULL,
    phrase_type VARCHAR(64) NOT NULL,
    hit_weight INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_policy_intent_phrase_intent
    ON ai_policy_intent_phrase(base_id, intent_id, enabled, hit_weight DESC);
CREATE INDEX IF NOT EXISTS idx_ai_policy_intent_phrase_trgm
    ON ai_policy_intent_phrase USING GIN (phrase gin_trgm_ops);

CREATE TABLE IF NOT EXISTS ai_policy_intent_answer (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    intent_id BIGINT NOT NULL,
    answer_mode VARCHAR(64) NOT NULL,
    answer_title VARCHAR(255),
    answer_template TEXT NOT NULL,
    evidence_rule TEXT,
    followup_suggestion TEXT,
    priority INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_policy_intent_answer_intent
    ON ai_policy_intent_answer(base_id, intent_id, answer_mode, enabled, priority DESC);

CREATE TABLE IF NOT EXISTS ai_policy_user_favorite (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    session_id BIGINT,
    raw_question TEXT,
    normalized_question TEXT,
    policy_key VARCHAR(128),
    intent_id BIGINT,
    final_answer TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_policy_user_favorite_base_user
    ON ai_policy_user_favorite(base_id, user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_policy_user_favorite_intent
    ON ai_policy_user_favorite(base_id, intent_id, created_at DESC);

CREATE TABLE IF NOT EXISTS ai_policy_feedback (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    user_id BIGINT,
    session_id BIGINT,
    raw_question TEXT,
    normalized_question TEXT,
    intent_id BIGINT,
    feedback_type VARCHAR(64) NOT NULL,
    feedback_text TEXT,
    final_answer TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_policy_feedback_base_type
    ON ai_policy_feedback(base_id, feedback_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_policy_feedback_intent
    ON ai_policy_feedback(base_id, intent_id, created_at DESC);

CREATE TABLE IF NOT EXISTS ai_policy_candidate_phrase (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    region_scope VARCHAR(64),
    policy_key VARCHAR(128),
    raw_question TEXT,
    normalized_question TEXT NOT NULL,
    suggested_intent_id BIGINT,
    source_type VARCHAR(64) NOT NULL,
    review_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_policy_candidate_phrase_status
    ON ai_policy_candidate_phrase(base_id, review_status, updated_at DESC);

CREATE TABLE IF NOT EXISTS ai_policy_candidate_answer (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    region_scope VARCHAR(64),
    policy_key VARCHAR(128),
    intent_id BIGINT,
    raw_question TEXT,
    final_answer TEXT NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    review_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_policy_candidate_answer_status
    ON ai_policy_candidate_answer(base_id, review_status, updated_at DESC);

CREATE TABLE IF NOT EXISTS ai_policy_intent_suggest_log (
    id BIGSERIAL PRIMARY KEY,
    base_id BIGINT NOT NULL,
    user_id BIGINT,
    raw_input TEXT,
    normalized_input TEXT,
    suggested_items TEXT,
    selected_intent_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_policy_intent_suggest_log_base
    ON ai_policy_intent_suggest_log(base_id, created_at DESC);

ALTER TABLE ai_policy_answer_log
    ADD COLUMN IF NOT EXISTS matched_intent_id BIGINT,
    ADD COLUMN IF NOT EXISTS matched_intent_code VARCHAR(255),
    ADD COLUMN IF NOT EXISTS suggested_intents TEXT,
    ADD COLUMN IF NOT EXISTS selected_intent_id BIGINT,
    ADD COLUMN IF NOT EXISTS learning_signal_written BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS candidate_generated BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE ai_policy_retrieval_log
    ADD COLUMN IF NOT EXISTS matched_intent_id BIGINT,
    ADD COLUMN IF NOT EXISTS matched_intent_code VARCHAR(255),
    ADD COLUMN IF NOT EXISTS suggested_intents TEXT,
    ADD COLUMN IF NOT EXISTS selected_intent_id BIGINT,
    ADD COLUMN IF NOT EXISTS learning_signal_written BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS candidate_generated BOOLEAN NOT NULL DEFAULT FALSE;
