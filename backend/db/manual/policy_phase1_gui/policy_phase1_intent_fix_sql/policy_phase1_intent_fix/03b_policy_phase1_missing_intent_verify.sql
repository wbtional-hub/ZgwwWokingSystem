/*
03b_policy_phase1_missing_intent_verify.sql
用途：验证 02b 是否补齐缺失 intent 与精准短语。
*/

-- 1. 查看本轮新增 intent
select
  policy_key,
  region_scope,
  intent_code,
  intent_name,
  question_type,
  topic_type,
  answer_level_default,
  priority,
  enabled
from ai_policy_intent
where intent_code in (
  'phase1_double_hundred_material',
  'phase1_double_hundred_department',
  'phase1_xiamen_finance_material',
  'phase1_xiamen_finance_certificate_benefit',
  'phase1_postdoc_station',
  'phase1_postdoc_city_province_boundary'
)
order by policy_key, topic_type, intent_code;

-- 2. 查看本轮新增精准短语
select
  i.policy_key,
  i.topic_type,
  i.intent_code,
  p.phrase,
  p.hit_weight,
  p.phrase_type,
  p.enabled
from ai_policy_intent_phrase p
join ai_policy_intent i on i.id = p.intent_id
where p.phrase_type = 'POLICY_STANDARDIZATION_PHASE1_INTENT_FIX'
order by i.policy_key, i.topic_type, p.phrase;

-- 3. 再次检查缺失 intent 是否还存在
with required_topic as (
  select *
  from (
    values
    ('double_hundred','material','双百计划需要什么材料'),
    ('double_hundred','department','双百计划主管部门是谁'),
    ('xiamen_finance','material','金融服务产业人才项目需要什么材料'),
    ('xiamen_finance','benefit','CFA 可以申请金融服务产业人才补贴吗'),
    ('postdoc','station','博士后工作站怎么申请'),
    ('postdoc','city_province_boundary','厦门博士后和福建博士后有什么区别')
  ) t(policy_key, topic_type, phrase)
)
select r.policy_key, r.topic_type, r.phrase
from required_topic r
where not exists (
  select 1
  from ai_policy_intent i
  where i.policy_key = r.policy_key
    and i.topic_type = r.topic_type
    and i.enabled = true
)
order by r.policy_key, r.topic_type, r.phrase;
