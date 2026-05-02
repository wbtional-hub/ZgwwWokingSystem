/*
99b_policy_phase1_missing_intent_rollback.sql
用途：仅回滚 02b 新增的 intent phrase 和 intent。
注意：先删 phrase，再删 intent。
*/

begin;

delete from ai_policy_intent_phrase
where phrase_type = 'POLICY_STANDARDIZATION_PHASE1_INTENT_FIX';

delete from ai_policy_intent
where intent_code in (
  'phase1_double_hundred_material',
  'phase1_double_hundred_department',
  'phase1_xiamen_finance_material',
  'phase1_xiamen_finance_certificate_benefit',
  'phase1_postdoc_station',
  'phase1_postdoc_city_province_boundary'
);

commit;
