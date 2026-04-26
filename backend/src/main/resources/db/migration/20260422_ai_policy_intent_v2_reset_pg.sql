DELETE FROM ai_policy_intent_suggest_log WHERE base_id = ${BASE_ID};
DELETE FROM ai_policy_candidate_answer WHERE base_id = ${BASE_ID};
DELETE FROM ai_policy_candidate_phrase WHERE base_id = ${BASE_ID};
DELETE FROM ai_policy_feedback WHERE base_id = ${BASE_ID};
DELETE FROM ai_policy_user_favorite WHERE base_id = ${BASE_ID};
DELETE FROM ai_policy_intent_answer WHERE base_id = ${BASE_ID};
DELETE FROM ai_policy_intent_phrase WHERE base_id = ${BASE_ID};
DELETE FROM ai_policy_intent WHERE base_id = ${BASE_ID};

TRUNCATE TABLE ai_policy_intent_suggest_log RESTART IDENTITY CASCADE;
TRUNCATE TABLE ai_policy_candidate_answer RESTART IDENTITY CASCADE;
TRUNCATE TABLE ai_policy_candidate_phrase RESTART IDENTITY CASCADE;
TRUNCATE TABLE ai_policy_feedback RESTART IDENTITY CASCADE;
TRUNCATE TABLE ai_policy_user_favorite RESTART IDENTITY CASCADE;
TRUNCATE TABLE ai_policy_intent_answer RESTART IDENTITY CASCADE;
TRUNCATE TABLE ai_policy_intent_phrase RESTART IDENTITY CASCADE;
TRUNCATE TABLE ai_policy_intent RESTART IDENTITY CASCADE;
