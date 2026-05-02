-- 03_policy_phase1_verify.sql
-- GUI兼容版：已去掉 psql 专用 \echo。
-- 用途：执行 02 后验证新增数据。只读，不修改任何数据。

select '==== 1. ai_policy_document 验证 ====' as section;
select policy_key, count(*) cnt
from ai_policy_document
where policy_key in ('double_hundred', 'xiamen_finance', 'postdoc')
group by policy_key
order by policy_key;

select '==== 2. ai_policy_chunk 按专题类型验证 ====' as section;
select policy_key, topic_type, count(*) cnt
from ai_policy_chunk
where policy_key in ('double_hundred', 'xiamen_finance', 'postdoc')
group by policy_key, topic_type
order by policy_key, topic_type;

select '==== 3. 本轮新增 FAQ 验证 ====' as section;
select policy_key, slot_code, standard_question
from ai_policy_faq
where coalesce(evidence_source, '') like '%POLICY_STANDARDIZATION_PHASE1%'
order by policy_key, slot_code;

select '==== 4. 本轮新增 intent phrase 验证 ====' as section;
select i.policy_key, i.topic_type, p.phrase
from ai_policy_intent_phrase p
join ai_policy_intent i on i.id = p.intent_id
where p.phrase_type = 'POLICY_STANDARDIZATION_PHASE1'
order by i.policy_key, i.topic_type, p.phrase;

select '==== 5. 本轮新增 condition_index 验证 ====' as section;
select policy_name, policy_no, condition_type, condition_code, condition_text
from ai_policy_condition_index
where coalesce(source_text, '') like '%POLICY_STANDARDIZATION_PHASE1%'
order by policy_name, condition_type, condition_code;

select '==== 6. 推荐预期 ====' as section;
select '预期：document 至少 3 条；chunk 约 26 条；FAQ 约 8 条；condition_index 约 22 条；intent_phrase 视现有 intent 情况插入。' as expected;
