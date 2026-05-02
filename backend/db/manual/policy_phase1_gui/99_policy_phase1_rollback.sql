-- 99_policy_phase1_rollback.sql
-- GUI兼容版：不含 \echo，不含 DO $$。
-- 用途：仅回滚本轮 POLICY_STANDARDIZATION_PHASE1 新增数据。
-- 注意：本文件包含 delete，只在需要回滚时执行。

begin;

delete from ai_policy_condition_index
where coalesce(source_text, '') like '%POLICY_STANDARDIZATION_PHASE1%';

delete from ai_policy_intent_phrase
where phrase_type = 'POLICY_STANDARDIZATION_PHASE1';

delete from ai_policy_faq
where coalesce(evidence_source, '') like '%POLICY_STANDARDIZATION_PHASE1%';

delete from ai_policy_chunk
where source_scene = 'POLICY_STANDARDIZATION_PHASE1';

delete from ai_policy_document
where source_type = 'POLICY_STANDARDIZATION_PHASE1';

commit;
