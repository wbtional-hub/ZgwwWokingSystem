-- 04_policy_phase1_evidence_candidates.sql
-- 用途：只读查询，寻找 matched_chunk_id 候选证据。
-- GUI兼容版：已去掉 \echo，且使用 to_jsonb 读取可选字段，避免 content_text / heading_path 字段名差异导致报错。
-- 注意：本文件只查询，不自动绑定，不修改任何数据。

select '==== double_hundred_process ====' as section;
select 'double_hundred_process' target,
       c.id,
       to_jsonb(c)->>'policy_name' as policy_name,
       to_jsonb(c)->>'region_scope' as region_scope,
       to_jsonb(c)->>'topic_type' as topic_type,
       to_jsonb(c)->>'heading_path' as heading_path,
       left(coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', ''), 220) as content_preview
from ai_knowledge_chunk c
where (coalesce(to_jsonb(c)->>'policy_name', '') like '%双百%'
    or coalesce(to_jsonb(c)->>'policy_aliases', '') like '%双百%'
    or coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') like '%双百计划%')
  and coalesce(to_jsonb(c)->>'topic_type', '') = 'process'
  and coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') ~ '申报|申请|流程|评审|确认|特聘岗位'
order by (coalesce(to_jsonb(c)->>'region_scope', '') = 'XM') desc, c.id
limit 10;

select '==== double_hundred_material ====' as section;
select 'double_hundred_material' target,
       c.id,
       to_jsonb(c)->>'policy_name' as policy_name,
       to_jsonb(c)->>'region_scope' as region_scope,
       to_jsonb(c)->>'topic_type' as topic_type,
       to_jsonb(c)->>'heading_path' as heading_path,
       left(coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', ''), 220) as content_preview
from ai_knowledge_chunk c
where (coalesce(to_jsonb(c)->>'policy_name', '') like '%双百%'
    or coalesce(to_jsonb(c)->>'policy_aliases', '') like '%双百%'
    or coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') like '%双百计划%')
  and coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') ~ '材料|申报表|证明|承诺书|提交'
order by (coalesce(to_jsonb(c)->>'region_scope', '') = 'XM') desc, c.id
limit 10;

select '==== double_hundred_department ====' as section;
select 'double_hundred_department' target,
       c.id,
       to_jsonb(c)->>'policy_name' as policy_name,
       to_jsonb(c)->>'region_scope' as region_scope,
       to_jsonb(c)->>'topic_type' as topic_type,
       to_jsonb(c)->>'heading_path' as heading_path,
       left(coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', ''), 220) as content_preview
from ai_knowledge_chunk c
where (coalesce(to_jsonb(c)->>'policy_name', '') like '%双百%'
    or coalesce(to_jsonb(c)->>'policy_aliases', '') like '%双百%'
    or coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') like '%双百计划%')
  and coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') ~ '主管部门|受理|组织|推荐|单位'
order by (coalesce(to_jsonb(c)->>'region_scope', '') = 'XM') desc, c.id
limit 10;

select '==== xiamen_finance_process ====' as section;
select 'xiamen_finance_process' target,
       c.id,
       to_jsonb(c)->>'policy_name' as policy_name,
       to_jsonb(c)->>'region_scope' as region_scope,
       to_jsonb(c)->>'topic_type' as topic_type,
       to_jsonb(c)->>'heading_path' as heading_path,
       left(coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', ''), 220) as content_preview
from ai_knowledge_chunk c
where (coalesce(to_jsonb(c)->>'policy_name', '') like '%金融服务产业人才%'
    or coalesce(to_jsonb(c)->>'policy_aliases', '') like '%金融服务产业人才%'
    or coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') like '%厦委人才办〔2025〕8号%')
  and coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') ~ '申报|申请|流程|程序|申报方式|受理|审核'
order by (coalesce(to_jsonb(c)->>'region_scope', '') = 'XM') desc, c.id
limit 10;

select '==== xiamen_finance_material ====' as section;
select 'xiamen_finance_material' target,
       c.id,
       to_jsonb(c)->>'policy_name' as policy_name,
       to_jsonb(c)->>'region_scope' as region_scope,
       to_jsonb(c)->>'topic_type' as topic_type,
       to_jsonb(c)->>'heading_path' as heading_path,
       left(coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', ''), 220) as content_preview
from ai_knowledge_chunk c
where (coalesce(to_jsonb(c)->>'policy_name', '') like '%金融服务产业人才%'
    or coalesce(to_jsonb(c)->>'policy_aliases', '') like '%金融服务产业人才%'
    or coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') like '%厦委人才办〔2025〕8号%')
  and coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') ~ '材料|证明|申报表|资格证书|提交'
order by (coalesce(to_jsonb(c)->>'region_scope', '') = 'XM') desc, c.id
limit 10;

select '==== xiamen_finance_benefit ====' as section;
select 'xiamen_finance_benefit' target,
       c.id,
       to_jsonb(c)->>'policy_name' as policy_name,
       to_jsonb(c)->>'region_scope' as region_scope,
       to_jsonb(c)->>'topic_type' as topic_type,
       to_jsonb(c)->>'heading_path' as heading_path,
       left(coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', ''), 220) as content_preview
from ai_knowledge_chunk c
where (coalesce(to_jsonb(c)->>'policy_name', '') like '%金融服务产业人才%'
    or coalesce(to_jsonb(c)->>'policy_aliases', '') like '%金融服务产业人才%'
    or coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') like '%厦委人才办〔2025〕8号%')
  and coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') ~ '补贴|补助|支持|CFA|FRM|ACCA|FSA|万元'
order by (coalesce(to_jsonb(c)->>'region_scope', '') = 'XM') desc, c.id
limit 10;

select '==== postdoc_process ====' as section;
select 'postdoc_process' target,
       c.id,
       to_jsonb(c)->>'policy_name' as policy_name,
       to_jsonb(c)->>'region_scope' as region_scope,
       to_jsonb(c)->>'topic_type' as topic_type,
       to_jsonb(c)->>'heading_path' as heading_path,
       left(coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', ''), 220) as content_preview
from ai_knowledge_chunk c
where (coalesce(to_jsonb(c)->>'policy_name', '') like '%博士后%'
    or coalesce(to_jsonb(c)->>'policy_aliases', '') like '%博士后%'
    or coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') like '%厦人社〔2024〕233号%')
  and coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') ~ '申请|进站|提交|流程|审核|设站单位'
order by (coalesce(to_jsonb(c)->>'region_scope', '') = 'XM') desc,
         (coalesce(to_jsonb(c)->>'region_scope', '') = 'UNKNOWN') desc,
         c.id
limit 10;

select '==== postdoc_benefit ====' as section;
select 'postdoc_benefit' target,
       c.id,
       to_jsonb(c)->>'policy_name' as policy_name,
       to_jsonb(c)->>'region_scope' as region_scope,
       to_jsonb(c)->>'topic_type' as topic_type,
       to_jsonb(c)->>'heading_path' as heading_path,
       left(coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', ''), 220) as content_preview
from ai_knowledge_chunk c
where (coalesce(to_jsonb(c)->>'policy_name', '') like '%博士后%'
    or coalesce(to_jsonb(c)->>'policy_aliases', '') like '%博士后%'
    or coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') like '%厦人社〔2024〕233号%')
  and coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') ~ '补助|补贴|安家|在站|出站|万元'
order by (coalesce(to_jsonb(c)->>'region_scope', '') = 'XM') desc,
         (coalesce(to_jsonb(c)->>'region_scope', '') = 'UNKNOWN') desc,
         c.id
limit 10;

select '==== postdoc_station ====' as section;
select 'postdoc_station' target,
       c.id,
       to_jsonb(c)->>'policy_name' as policy_name,
       to_jsonb(c)->>'region_scope' as region_scope,
       to_jsonb(c)->>'topic_type' as topic_type,
       to_jsonb(c)->>'heading_path' as heading_path,
       left(coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', ''), 220) as content_preview
from ai_knowledge_chunk c
where (coalesce(to_jsonb(c)->>'policy_name', '') like '%博士后%'
    or coalesce(to_jsonb(c)->>'policy_aliases', '') like '%博士后%'
    or coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') like '%博士后工作站%')
  and coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') ~ '工作站|科研工作站|设站|单位|招收'
order by (coalesce(to_jsonb(c)->>'region_scope', '') = 'XM') desc,
         (coalesce(to_jsonb(c)->>'region_scope', '') = 'UNKNOWN') desc,
         c.id
limit 10;

select '==== postdoc_city_province_boundary ====' as section;
select 'postdoc_city_province_boundary' target,
       c.id,
       to_jsonb(c)->>'policy_name' as policy_name,
       to_jsonb(c)->>'region_scope' as region_scope,
       to_jsonb(c)->>'topic_type' as topic_type,
       to_jsonb(c)->>'heading_path' as heading_path,
       left(coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', ''), 220) as content_preview
from ai_knowledge_chunk c
where (coalesce(to_jsonb(c)->>'policy_name', '') like '%博士后%'
    or coalesce(to_jsonb(c)->>'policy_aliases', '') like '%博士后%'
    or coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') like '%博士后%')
  and (coalesce(to_jsonb(c)->>'region_scope', '') in ('XM','FJ','UNKNOWN')
       or coalesce(to_jsonb(c)->>'content_text', to_jsonb(c)->>'content', '') ~ '厦门|福建|省级|市级')
order by (coalesce(to_jsonb(c)->>'region_scope', '') = 'XM') desc,
         (coalesce(to_jsonb(c)->>'region_scope', '') = 'FJ') desc,
         c.id
limit 20;
