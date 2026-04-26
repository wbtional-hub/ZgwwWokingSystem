/* 仅清理 AI 政策咨询独立子系统数据，请先替换 ${BASE_ID} */
DELETE FROM ai_policy_answer_log WHERE base_id = 1;
DELETE FROM ai_policy_retrieval_log WHERE base_id = 1;
DELETE FROM ai_policy_eval_case WHERE base_id = 1;
DELETE FROM ai_policy_route_rule WHERE base_id = 1;
DELETE FROM ai_policy_faq WHERE base_id = 1;
DELETE FROM ai_policy_alias WHERE base_id = 1;
DELETE FROM ai_policy_chunk WHERE base_id = 1;
DELETE FROM ai_policy_document WHERE base_id = 1;
