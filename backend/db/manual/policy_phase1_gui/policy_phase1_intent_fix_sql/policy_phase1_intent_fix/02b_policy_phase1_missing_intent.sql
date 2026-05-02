/*
02b_policy_phase1_missing_intent.sql
用途：补齐第一阶段验证中发现的缺失 intent，并把自然问法挂到更精准的 topic_type。
限制：
1. 只 insert，不 update / delete / truncate / drop / alter。
2. 使用 POLICY_STANDARDIZATION_PHASE1_INTENT_FIX 标记，便于单独验证和回滚。
3. 不删除上一轮 fallback 短语；通过更高 priority / hit_weight 让精准 intent 优先命中。
4. 执行前建议先备份数据库，或至少确认 03_policy_phase1_verify.sql 已通过。
*/

begin;

-- 1. 补缺失 intent
with rows as (
  select *
  from (
    values
    -- policy_key, topic_type, intent_code, intent_name, standard_question, question_type, answer_level_default, priority
    ('double_hundred', 'material', 'phase1_double_hundred_material', '双百计划材料问法', '双百计划需要什么材料', 'MATERIAL', 'standard', 160),
    ('double_hundred', 'department', 'phase1_double_hundred_department', '双百计划主管部门问法', '双百计划主管部门是谁', 'ROUTE_HELP', 'standard', 150),

    ('xiamen_finance', 'material', 'phase1_xiamen_finance_material', '金融服务产业人才项目材料问法', '金融服务产业人才项目需要什么材料', 'MATERIAL', 'standard', 160),
    ('xiamen_finance', 'benefit', 'phase1_xiamen_finance_certificate_benefit', '金融专业资格补贴问法', 'CFA 可以申请金融服务产业人才补贴吗', 'BENEFIT', 'standard', 155),

    ('postdoc', 'station', 'phase1_postdoc_station', '博士后工作站问法', '博士后工作站怎么申请', 'PROCESS', 'standard', 160),
    ('postdoc', 'city_province_boundary', 'phase1_postdoc_city_province_boundary', '博士后省市边界问法', '厦门博士后和福建博士后有什么区别', 'BOUNDARY', 'standard', 155)
  ) as t(policy_key, topic_type, intent_code, intent_name, standard_question, question_type, answer_level_default, priority)
),
region_pick as (
  select
    r.*,
    coalesce(
      (
        select i.region_scope
        from ai_policy_intent i
        where i.policy_key = r.policy_key
          and i.region_scope is not null
          and trim(i.region_scope) <> ''
        group by i.region_scope
        order by count(*) desc, i.region_scope
        limit 1
      ),
      'XM'
    ) as region_scope
  from rows r
)
insert into ai_policy_intent
(base_id, region_scope, policy_key, intent_code, intent_name, standard_question, question_type, topic_type, answer_level_default, priority, enabled, created_at, updated_at)
select
  1,
  r.region_scope,
  r.policy_key,
  r.intent_code,
  r.intent_name,
  r.standard_question,
  r.question_type,
  r.topic_type,
  r.answer_level_default,
  r.priority,
  true,
  now(),
  now()
from region_pick r
where not exists (
  select 1
  from ai_policy_intent old
  where old.policy_key = r.policy_key
    and old.topic_type = r.topic_type
    and old.intent_code = r.intent_code
);

-- 2. 把自然问法挂到更精准的新 intent 上
with phrase_rows as (
  select *
  from (
    values
    ('double_hundred', 'phase1_double_hundred_material', '双百计划需要什么材料'),
    ('double_hundred', 'phase1_double_hundred_department', '双百计划主管部门是谁'),

    ('xiamen_finance', 'phase1_xiamen_finance_material', '金融服务产业人才项目需要什么材料'),
    ('xiamen_finance', 'phase1_xiamen_finance_certificate_benefit', 'CFA 可以申请金融服务产业人才补贴吗'),

    ('postdoc', 'phase1_postdoc_station', '博士后工作站怎么申请'),
    ('postdoc', 'phase1_postdoc_city_province_boundary', '厦门博士后和福建博士后有什么区别')
  ) as t(policy_key, intent_code, phrase)
),
picked as (
  select r.policy_key, r.phrase, i.id as intent_id
  from phrase_rows r
  join ai_policy_intent i
    on i.policy_key = r.policy_key
   and i.intent_code = r.intent_code
   and i.enabled = true
)
insert into ai_policy_intent_phrase
(base_id, intent_id, phrase, phrase_type, hit_weight, enabled, created_at, updated_at)
select
  1,
  p.intent_id,
  p.phrase,
  'POLICY_STANDARDIZATION_PHASE1_INTENT_FIX',
  120,
  true,
  now(),
  now()
from picked p
where not exists (
  select 1
  from ai_policy_intent_phrase old
  where old.intent_id = p.intent_id
    and old.phrase = p.phrase
);

commit;
