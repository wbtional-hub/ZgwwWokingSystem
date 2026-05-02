-- 01_policy_phase1_check.sql
-- 用途：执行前核查。只读，不修改业务数据。
-- GUI兼容版：已去掉 psql 专用的 \echo，也去掉 DO $$ 动态块，适合 Navicat / DBeaver / DataGrip / pgAdmin 查询窗口执行。
-- 执行后请把结果发给 ChatGPT，再决定是否执行 02 插入脚本。

select '==== 1. 核查真实字段：核心政策表与旧知识库表 ====' as section;

select table_name, ordinal_position, column_name, data_type, is_nullable, column_default
from information_schema.columns
where table_schema = 'public'
  and table_name in (
    'ai_policy_document',
    'ai_policy_chunk',
    'ai_policy_faq',
    'ai_policy_alias',
    'ai_policy_intent',
    'ai_policy_intent_phrase',
    'ai_policy_condition_index',
    'ai_knowledge_document',
    'ai_knowledge_chunk',
    'ai_policy_catalog'
  )
order by table_name, ordinal_position;

select '==== 2. 关键字段是否存在：用于判断 02 脚本能否执行 ====' as section;

with required(table_name, column_name, remark) as (
  values
  ('ai_policy_document','base_id','02 插入需要'),
  ('ai_policy_document','name','02 插入需要'),
  ('ai_policy_document','region_scope','02 插入需要'),
  ('ai_policy_document','policy_key','02 插入需要'),
  ('ai_policy_document','policy_name','02 插入需要'),
  ('ai_policy_document','doc_type','02 插入需要'),
  ('ai_policy_document','source_type','02 插入需要'),
  ('ai_policy_document','source_file_name','02 插入需要'),
  ('ai_policy_document','enabled','02 插入需要'),
  ('ai_policy_document','created_at','02 默认版本使用；如果不存在但 create_time 存在，需要换字段版本'),
  ('ai_policy_document','updated_at','02 默认版本使用；如果不存在但 update_time 存在，需要换字段版本'),

  ('ai_policy_chunk','base_id','02 插入需要'),
  ('ai_policy_chunk','document_id','02 插入需要'),
  ('ai_policy_chunk','region_scope','02 插入需要'),
  ('ai_policy_chunk','policy_key','02 插入需要'),
  ('ai_policy_chunk','policy_name','02 插入需要'),
  ('ai_policy_chunk','policy_aliases','02 插入需要'),
  ('ai_policy_chunk','doc_type','02 插入需要'),
  ('ai_policy_chunk','topic_type','02 插入需要'),
  ('ai_policy_chunk','question_type','02 插入需要'),
  ('ai_policy_chunk','title','02 插入需要'),
  ('ai_policy_chunk','content','02 插入需要'),
  ('ai_policy_chunk','answer_level','02 插入需要'),
  ('ai_policy_chunk','priority','02 插入需要'),
  ('ai_policy_chunk','source_scene','02 插入需要'),
  ('ai_policy_chunk','sort_no','02 插入需要'),
  ('ai_policy_chunk','enabled','02 插入需要'),
  ('ai_policy_chunk','created_at','02 默认版本使用；如果不存在但 create_time 存在，需要换字段版本'),
  ('ai_policy_chunk','updated_at','02 默认版本使用；如果不存在但 update_time 存在，需要换字段版本'),

  ('ai_policy_faq','base_id','02 插入需要'),
  ('ai_policy_faq','region_scope','02 插入需要'),
  ('ai_policy_faq','policy_key','02 插入需要'),
  ('ai_policy_faq','policy_name','02 插入需要'),
  ('ai_policy_faq','slot_code','02 插入需要'),
  ('ai_policy_faq','question_pattern','02 插入需要'),
  ('ai_policy_faq','standard_question','02 插入需要'),
  ('ai_policy_faq','standard_answer','02 插入需要'),
  ('ai_policy_faq','evidence_source','02 插入需要'),
  ('ai_policy_faq','priority','02 插入需要'),
  ('ai_policy_faq','enabled','02 插入需要'),
  ('ai_policy_faq','created_at','02 默认版本使用；如果不存在但 create_time 存在，需要换字段版本'),
  ('ai_policy_faq','updated_at','02 默认版本使用；如果不存在但 update_time 存在，需要换字段版本'),

  ('ai_policy_intent','id','短语挂载需要'),
  ('ai_policy_intent','policy_key','短语挂载需要'),
  ('ai_policy_intent','topic_type','短语挂载需要'),
  ('ai_policy_intent','priority','短语挂载需要'),
  ('ai_policy_intent','enabled','短语挂载需要'),

  ('ai_policy_intent_phrase','base_id','02 插入需要'),
  ('ai_policy_intent_phrase','intent_id','02 插入需要'),
  ('ai_policy_intent_phrase','phrase','02 插入需要'),
  ('ai_policy_intent_phrase','phrase_type','02 插入需要'),
  ('ai_policy_intent_phrase','hit_weight','02 插入需要'),
  ('ai_policy_intent_phrase','enabled','02 插入需要'),
  ('ai_policy_intent_phrase','created_at','02 默认版本使用；如果不存在但 create_time 存在，需要换字段版本'),
  ('ai_policy_intent_phrase','updated_at','02 默认版本使用；如果不存在但 update_time 存在，需要换字段版本'),

  ('ai_policy_condition_index','base_id','02 插入需要'),
  ('ai_policy_condition_index','policy_name','02 插入需要'),
  ('ai_policy_condition_index','policy_no','02 插入需要'),
  ('ai_policy_condition_index','policy_aliases','02 插入需要'),
  ('ai_policy_condition_index','condition_type','02 插入需要'),
  ('ai_policy_condition_index','condition_code','02 插入需要'),
  ('ai_policy_condition_index','condition_text','02 插入需要'),
  ('ai_policy_condition_index','requirement_text','02 插入需要'),
  ('ai_policy_condition_index','matched_chunk_id','02 插入需要'),
  ('ai_policy_condition_index','source_text','02 插入需要'),
  ('ai_policy_condition_index','priority','02 插入需要'),
  ('ai_policy_condition_index','status','02 插入需要'),
  ('ai_policy_condition_index','created_at','02 默认版本使用；如果不存在但 create_time 存在，需要换字段版本'),
  ('ai_policy_condition_index','updated_at','02 默认版本使用；如果不存在但 update_time 存在，需要换字段版本')
), actual as (
  select table_name, column_name, data_type
  from information_schema.columns
  where table_schema = 'public'
)
select r.table_name,
       r.column_name,
       case when a.column_name is null then 'MISSING' else 'OK' end as check_result,
       a.data_type,
       r.remark
from required r
left join actual a
  on a.table_name = r.table_name
 and a.column_name = r.column_name
order by r.table_name, r.column_name;

select '==== 3. 核查 3 个 policy_key 现有数据，防重复 ====' as section;

select 'ai_policy_document' as table_name, policy_key, count(*) cnt
from ai_policy_document
where policy_key in ('double_hundred', 'xiamen_finance', 'postdoc')
group by policy_key
union all
select 'ai_policy_chunk' as table_name, policy_key, count(*) cnt
from ai_policy_chunk
where policy_key in ('double_hundred', 'xiamen_finance', 'postdoc')
group by policy_key
union all
select 'ai_policy_faq' as table_name, policy_key, count(*) cnt
from ai_policy_faq
where policy_key in ('double_hundred', 'xiamen_finance', 'postdoc')
group by policy_key
union all
select 'ai_policy_alias' as table_name, policy_key, count(*) cnt
from ai_policy_alias
where policy_key in ('double_hundred', 'xiamen_finance', 'postdoc')
group by policy_key
union all
select 'ai_policy_intent' as table_name, policy_key, count(*) cnt
from ai_policy_intent
where policy_key in ('double_hundred', 'xiamen_finance', 'postdoc')
group by policy_key
order by table_name, policy_key;

select '==== 4. 安全核查 region_scope / policy_region 实际口径：使用 to_jsonb，字段不存在也不会报错 ====' as section;

select source_table, source_column, value, count(*) cnt
from (
  select 'ai_knowledge_document' as source_table,
         'policy_region' as source_column,
         coalesce(to_jsonb(d)->>'policy_region', '<NULL>') as value
  from ai_knowledge_document d
  union all
  select 'ai_knowledge_document',
         'region_scope',
         coalesce(to_jsonb(d)->>'region_scope', '<NULL>')
  from ai_knowledge_document d
  union all
  select 'ai_knowledge_chunk',
         'region_scope',
         coalesce(to_jsonb(c)->>'region_scope', '<NULL>')
  from ai_knowledge_chunk c
  union all
  select 'ai_policy_faq',
         'region_scope',
         coalesce(to_jsonb(f)->>'region_scope', '<NULL>')
  from ai_policy_faq f
  union all
  select 'ai_policy_alias',
         'region_scope',
         coalesce(to_jsonb(a)->>'region_scope', '<NULL>')
  from ai_policy_alias a
  where a.policy_key in ('double_hundred', 'xiamen_finance', 'postdoc')
) x
group by source_table, source_column, value
order by source_table, source_column, value;

select '==== 5. 双百计划文号候选核查：使用 to_jsonb，字段不存在也不会报错 ====' as section;

select source_table, policy_no, policy_name, preview
from (
  select 'ai_knowledge_chunk' as source_table,
         nullif(to_jsonb(c)->>'policy_no', '') as policy_no,
         to_jsonb(c)->>'policy_name' as policy_name,
         left(coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', ''), 160) as preview
  from ai_knowledge_chunk c
  where coalesce(to_jsonb(c)->>'policy_name', '') like '%双百%'
     or coalesce(to_jsonb(c)->>'policy_aliases', '') like '%双百%'
     or coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') like '%双百计划%'
  union all
  select 'ai_policy_catalog' as source_table,
         nullif(to_jsonb(pc)->>'policy_no', '') as policy_no,
         to_jsonb(pc)->>'policy_name' as policy_name,
         left(coalesce(to_jsonb(pc)->>'policy_aliases', ''), 160) as preview
  from ai_policy_catalog pc
  where coalesce(to_jsonb(pc)->>'policy_name', '') like '%双百%'
     or coalesce(to_jsonb(pc)->>'policy_aliases', '') like '%双百%'
) x
order by source_table, policy_name, policy_no
limit 40;

select '==== 6. 缺失 intent 检查：如果有结果，不影响主数据插入，但对应问法可能挂不上 ====' as section;

with required_topic as (
  select *
  from (
    values
    ('double_hundred','process','双百计划怎么申请'),
    ('double_hundred','material','双百计划需要什么材料'),
    ('double_hundred','department','双百计划主管部门是谁'),
    ('xiamen_finance','process','金融服务产业人才怎么申请'),
    ('xiamen_finance','material','金融服务产业人才项目需要什么材料'),
    ('xiamen_finance','benefit','CFA 可以申请金融服务产业人才补贴吗'),
    ('xiamen_finance','condition','CFA 可以申请金融服务产业人才补贴吗'),
    ('postdoc','benefit','博士后补助怎么申请'),
    ('postdoc','process','博士后补助怎么申请'),
    ('postdoc','station','博士后工作站怎么申请'),
    ('postdoc','process','博士后工作站怎么申请'),
    ('postdoc','city_province_boundary','厦门博士后和福建博士后有什么区别'),
    ('postdoc','route_help','厦门博士后和福建博士后有什么区别')
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

select '==== 7. 重要提醒 ====' as section;
select '如果 region_scope 不是 XM，或者核心表时间字段不是 created_at / updated_at，请先不要执行 02_policy_phase1_insert.sql。' as notice;
