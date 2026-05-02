-- 02_policy_phase1_insert.sql
-- GUI兼容版：不含 \echo，不含 DO $$。
-- 用途：补齐三类样板政策的标准化专题卡数据。
-- 注意：
-- 1. 执行前必须先执行 01_policy_phase1_check.sql。
-- 2. 本脚本仅 insert，不 update / delete / truncate / drop / alter。
-- 3. 本脚本统一使用 region_scope = 'XM'。
-- 4. 本脚本默认核心表使用 created_at / updated_at 字段。
-- 5. 双百计划 policy_no 本轮不硬写，统一置为 null，避免未经确认文号入库。
-- 6. 回滚请执行 99_policy_phase1_rollback.sql。

begin;

-- 1. ai_policy_document：三条专题文档
insert into ai_policy_document
(base_id, name, region_scope, policy_key, policy_name, doc_type, source_type, source_file_name, enabled, created_at, updated_at)
select 1, v.name, 'XM', v.policy_key, v.policy_name, 'topic', 'POLICY_STANDARDIZATION_PHASE1', '旧知识库标准化整理', true, now(), now()
from (
  values
  ('double_hundred', '厦门市引进高层次创新创业人才“双百计划”标准化专题卡', '厦门市引进高层次创新创业人才“双百计划”实施意见'),
  ('xiamen_finance', '厦门市金融服务产业人才项目标准化专题卡', '厦门市金融服务产业人才项目实施办法'),
  ('postdoc', '厦门市博士后政策标准化专题卡', '关于进一步加强博士后工作的若干措施')
) v(policy_key, name, policy_name)
where not exists (
  select 1 from ai_policy_document d
  where d.policy_key = v.policy_key
    and d.source_type = 'POLICY_STANDARDIZATION_PHASE1'
);

-- 2. ai_policy_chunk：标准化专题卡
with doc_map as (
  select policy_key, min(id) document_id
  from ai_policy_document
  where policy_key in ('double_hundred', 'xiamen_finance', 'postdoc')
    and source_type = 'POLICY_STANDARDIZATION_PHASE1'
  group by policy_key
),
rows as (
  select *
  from (
    values
    ('double_hundred','overview','双百计划是什么','适用问题：双百计划是什么、支持哪些人才。核心答复：双百计划面向厦门市引进高层次创新创业人才，通常区分创新个人、创新团队和创业人才。依据边界：具体类别、批次和标准以正式政策及年度申报通知为准。办理建议：先确认人才类别，再看条件、流程、补助和材料。'),
    ('double_hundred','condition','双百计划谁能申请','适用问题：谁能申请双百计划。核心答复：需区分创新个人、创新团队、创业人才，不同类别对应人才层次、项目成果、单位承载和落地要求。依据边界：不能仅凭政策名称判断符合。办理建议：按年度通知进行资格初筛。'),
    ('double_hundred','process','双百计划怎么申请','适用问题：双百计划怎么申请、申报流程。核心答复：创新个人通常通过高层次人才特聘岗位方式遴选；创新团队、创业人才通常包括组织申报、资格核查、部门联审、综合评审、研究确认等环节。依据边界：具体入口、时间、材料以年度通知为准。办理建议：先确认申报类别。'),
    ('double_hundred','benefit','双百计划支持什么','适用问题：双百计划补助或支持内容。核心答复：可能涉及人才项目支持、创业项目支持、专项资金和服务保障等，不同类别支持不同。依据边界：金额、拨付条件和兑现周期以正式文件及通知为准。办理建议：按创新个人、创新团队、创业人才分别核对。'),
    ('double_hundred','material','双百计划需要什么材料','适用问题：双百计划材料清单。核心答复：通常可能包括身份证明、学历学位、职称资质、项目成果、单位推荐、申报表、承诺书等。依据边界：不能替代年度通知材料清单。办理建议：以年度申报通知或主管部门最新要求为准。'),
    ('double_hundred','department','双百计划主管部门是谁','适用问题：主管部门、找谁办理。核心答复：双百计划属于厦门市高层次创新创业人才引进政策，具体受理单位、主管部门和入口以年度申报通知为准。依据边界：不写死窗口、网址和批次。办理建议：关注主管部门和承载单位通知。'),
    ('double_hundred','boundary','双百计划回答边界','适用问题：时间、入口、金额、材料细项。核心答复：没有年度申报通知时，不能把入口、截止时间、完整材料清单说成确定事实。依据边界：知识库未命中的细节以年度申报通知或主管部门最新要求为准。办理建议：先给流程框架，再核对最新通知。'),
    ('double_hundred','faq','双百计划常见问题','适用问题：双百计划 FAQ。核心答复：高频问题包括怎么申请、谁能申请、补助多少、不同类别流程是否一样、材料以什么为准。依据边界：FAQ 只回答稳定口径。办理建议：按问题类型进入对应专题卡。'),

    ('xiamen_finance','overview','金融服务产业人才项目是什么','适用问题：金融服务产业人才项目是什么。核心答复：项目面向厦门金融服务产业相关单位和人才，涉及金融机构、基金管理机构、地方金融组织、金融投资集团等方向。依据边界：具体对象和标准以实施办法及年度通知为准。办理建议：先确认机构类型和人才资质。'),
    ('xiamen_finance','condition','金融服务产业人才项目谁能申请','适用问题：金融服务产业人才条件。核心答复：通常结合所在机构类型、学历职称、专业资格、工作年限、在厦工作情况判断。依据边界：不能仅凭 CFA、FRM、ACCA、FSA 或学历直接判断必然符合。办理建议：由所在机构按年度通知初筛。'),
    ('xiamen_finance','process','金融服务产业人才项目怎么申请','适用问题：金融服务产业人才怎么申请。核心答复：通常由人才所在机构推荐申报，经过材料提交、主管部门受理、资格审核、评审或认定、公示确认、政策兑现等环节。依据边界：具体入口、时间、材料清单以年度申报通知或主管部门最新要求为准。办理建议：先由所在机构确认申报资格。'),
    ('xiamen_finance','benefit','金融服务产业人才项目支持什么','适用问题：金融服务产业人才补助和支持。核心答复：该项目不应被简化为资格证书补贴，可能同时涉及人才类别认定、服务保障和专业资格支持。依据边界：补贴金额、证书范围和工作年限以实施办法及年度通知为准。办理建议：按人才类别和证书类型分别核对。'),
    ('xiamen_finance','material','金融服务产业人才项目需要什么材料','适用问题：金融服务产业人才材料。核心答复：通常可能涉及申报表、身份证明、学历学位、职称或专业资格证书、劳动关系或在厦工作证明、所在机构推荐材料、业绩成果、承诺书等。依据边界：最终以年度通知材料清单为准。办理建议：由所在机构统一准备或推荐。'),
    ('xiamen_finance','department','金融服务产业人才项目主管部门','适用问题：主管部门、找谁办理。核心答复：旧知识库显示该项目主管部门口径涉及市委金融办。依据边界：具体受理窗口、申报系统和入口以年度通知为准。办理建议：关注主管部门及所在机构内部申报通知。'),
    ('xiamen_finance','boundary','金融服务产业人才项目回答边界','适用问题：入口、时间、材料细项、补贴金额。核心答复：没有年度通知时，只能给出保守流程和办理方向，不能编造入口、截止日期或完整材料清单。依据边界：资格证书补贴不能代表整个政策。办理建议：按机构类型、人才类型、证书资质、年度通知四步核对。'),
    ('xiamen_finance','faq','金融服务产业人才项目常见问题','适用问题：金融服务产业人才 FAQ。核心答复：常见问题包括怎么申请、谁能申请、CFA/FRM/ACCA/FSA 是否可申请补贴、材料、主管部门。依据边界：动态事项以年度通知为准。办理建议：先识别机构和人才方向。'),

    ('postdoc','overview','博士后政策是什么','适用问题：博士后政策是什么。核心答复：博士后政策通常围绕个人进站、在站支持、出站留厦、博士后工作站或科研工作站建设等内容。依据边界：厦门市政策、福建省政策和国家政策不能混为一谈。办理建议：回答厦门博士后时优先厦门口径。'),
    ('postdoc','condition','博士后谁能申请','适用问题：博士后条件。核心答复：个人进站通常关注博士学位、年龄、研究能力、设站单位接收等；设站单位关注资质、科研平台、管理制度和招收能力。依据边界：具体条件以厦门市政策、设站单位和年度通知为准。办理建议：先判断个人进站、在站补助、出站留厦还是设站申请。'),
    ('postdoc','process','厦门博士后怎么申请','适用问题：博士后个人进站怎么申请。核心答复：个人通常先联系设站单位或博士后科研工作站，确认研究方向和接收条件，再提交书面申请及相关证明材料，由设站单位按主管流程办理。依据边界：具体入口、批次、材料和审核流程以设站单位及主管部门最新要求为准。办理建议：先确定目标工作站和申请事项。'),
    ('postdoc','benefit','博士后有什么补助','适用问题：博士后补助、在站补助、出站留厦补贴。核心答复：可能涉及在站期间补助、出站留厦就业安家补贴、工作站建设或运行支持等方向。依据边界：金额、年限、条件和兑现方式必须以正式政策和年度通知为准。办理建议：区分个人补助、出站留厦和工作站支持。'),
    ('postdoc','material','博士后需要什么材料','适用问题：博士后材料。核心答复：通常可能涉及博士学位证明、身份证明、个人简历、研究计划、设站单位接收或推荐意见、劳动或聘用关系材料、补助申请表、承诺书等。依据边界：不能替代设站单位或年度通知材料清单。办理建议：以设站单位和主管部门最新要求为准。'),
    ('postdoc','department','博士后政策找谁办理','适用问题：博士后主管部门或办理单位。核心答复：个人进站和补助申请通常先对接设站单位或博士后科研工作站，再按厦门市博士后管理和主管部门要求办理。依据边界：具体主管部门、窗口和入口以最新通知为准。办理建议：先联系拟进站单位人才或科研管理部门。'),
    ('postdoc','boundary','博士后回答边界','适用问题：省市政策区别、金额、入口。核心答复：厦门市博士后政策、福建省博士后政策和国家博士后制度口径不同，不能混为一谈。依据边界：回答厦门问题时优先厦门口径；引用福建省内容必须明确为省级政策。办理建议：先确认用户问个人、工作站还是省市对比。'),
    ('postdoc','faq','博士后常见问题','适用问题：博士后 FAQ。核心答复：常见问题包括个人进站、工作站申请、在站补助、出站留厦补贴、材料、省市区别。依据边界：动态事项以年度通知和设站单位要求为准。办理建议：按个人进站、工作站、补助、边界四类路由。'),
    ('postdoc','station','博士后工作站怎么申请','适用问题：博士后工作站或科研工作站。核心答复：工作站类问题面向设站单位，通常关注单位资质、科研条件、博士后招收培养能力、管理制度、年度招收计划等。依据边界：设站条件、材料和流程以正式通知及主管部门要求为准。办理建议：单位先核对设站资格。'),
    ('postdoc','city_province_boundary','厦门博士后和福建博士后有什么区别','适用问题：厦门博士后和福建博士后区别。核心答复：厦门市政策服务在厦设站单位、在厦进站或留厦发展的博士后；福建省政策属于省级口径。依据边界：不能把省级补助、认定或申报流程直接当作厦门市政策。办理建议：先确认申报主体和工作地点。')
  ) t(policy_key, topic_type, title, content)
)
insert into ai_policy_chunk
(base_id, document_id, region_scope, policy_key, policy_name, policy_aliases, doc_type, topic_type, question_type, title, content, answer_level, priority, source_scene, sort_no, enabled, created_at, updated_at)
select
  1,
  d.document_id,
  'XM',
  r.policy_key,
  case r.policy_key
    when 'double_hundred' then '厦门市引进高层次创新创业人才“双百计划”实施意见'
    when 'xiamen_finance' then '厦门市金融服务产业人才项目实施办法'
    when 'postdoc' then '关于进一步加强博士后工作的若干措施'
  end,
  case r.policy_key
    when 'double_hundred' then '双百计划,双百,厦门双百,创新创业人才双百计划'
    when 'xiamen_finance' then '金融人才,金融服务产业人才,金融服务产业人才项目,厦委人才办〔2025〕8号'
    when 'postdoc' then '博士后,厦门博士后,博士后工作站,博士后科研工作站,厦人社〔2024〕233号'
  end,
  'topic',
  r.topic_type,
  'POLICY_QA',
  r.title,
  r.content,
  'standard',
  100,
  'POLICY_STANDARDIZATION_PHASE1',
  row_number() over (partition by r.policy_key order by r.topic_type, r.title),
  true,
  now(),
  now()
from rows r
join doc_map d on d.policy_key = r.policy_key
where not exists (
  select 1 from ai_policy_chunk c
  where c.policy_key = r.policy_key
    and c.topic_type = r.topic_type
    and c.title = r.title
    and c.source_scene = 'POLICY_STANDARDIZATION_PHASE1'
);

-- 3. FAQ：只补稳定口径
insert into ai_policy_faq
(base_id, region_scope, policy_key, policy_name, slot_code, question_pattern, standard_question, standard_answer, evidence_source, priority, enabled, created_at, updated_at)
select 1, 'XM', v.policy_key, v.policy_name, v.slot_code, v.question_pattern, v.standard_question, v.standard_answer,
       'POLICY_STANDARDIZATION_PHASE1；旧知识库标准化专题卡；动态事项以年度申报通知或主管部门最新要求为准',
       v.priority, true, now(), now()
from (
  values
  ('double_hundred','厦门市引进高层次创新创业人才“双百计划”实施意见','process_boundary','双百计划创新个人创新团队创业人才流程一样吗|双百计划不同类别怎么申请','双百计划创新个人、创新团队、创业人才流程是否一样','不完全一样。创新个人通常通过高层次人才特聘岗位方式遴选；创新团队、创业人才通常更强调项目、团队和落地情况，流程一般包括组织申报、资格核查、部门联审、综合评审、研究确认等。具体以年度申报通知为准。',130),
  ('double_hundred','厦门市引进高层次创新创业人才“双百计划”实施意见','material','双百计划材料以什么为准|双百计划需要什么材料','双百计划材料以什么为准','双百计划材料应以当年度正式申报通知和申报系统要求为准。通常可提前准备身份证明、学历学位、职称资质、项目成果、单位推荐、申报表和承诺书等，但不能替代正式材料清单。',128),
  ('xiamen_finance','厦门市金融服务产业人才项目实施办法','process','金融服务产业人才项目怎么申请|金融服务产业人才怎么申请|金融人才怎么申请','金融服务产业人才项目怎么申请','通常由人才所在机构推荐申报，经过材料提交、主管部门受理、资格审核、评审或认定、公示确认、政策兑现等环节。具体申报入口、时间、材料清单以年度申报通知或主管部门最新要求为准。',130),
  ('xiamen_finance','厦门市金融服务产业人才项目实施办法','material','金融服务产业人才项目需要什么材料|金融人才需要什么材料','金融服务产业人才项目需要什么材料','通常可能涉及申报表、身份证明、学历学位、职称或专业资格证书、劳动关系或在厦工作证明、所在机构推荐材料、业绩成果和承诺书等。最终以年度通知材料清单为准。',126),
  ('postdoc','关于进一步加强博士后工作的若干措施','process','博士后个人进站怎么申请|厦门博士后怎么申请','博士后个人进站怎么申请','个人通常先联系设站单位或博士后科研工作站，确认研究方向和接收条件，再提交书面申请及相关证明材料，由设站单位按主管流程办理进站、审核、备案或补助申领等事项。具体以设站单位和主管部门最新要求为准。',130),
  ('postdoc','关于进一步加强博士后工作的若干措施','station','博士后工作站怎么申请|博士后科研工作站怎么申请','博士后工作站怎么申请','工作站类申请面向设站单位，通常关注单位资质、科研条件、博士后招收培养能力、管理制度和年度招收计划。具体设站条件、材料和流程以正式通知及主管部门要求为准。',128),
  ('postdoc','关于进一步加强博士后工作的若干措施','benefit_process','博士后出站留厦补贴怎么申请|博士后留厦补贴怎么申请','博士后出站留厦补贴怎么申请','出站留厦补贴应先确认是否符合留厦就业、时间、单位和政策适用条件，再按厦门市博士后政策及年度通知提交申请。补贴金额、材料、入口和兑现方式以正式政策及主管部门最新要求为准。',126),
  ('postdoc','关于进一步加强博士后工作的若干措施','city_province_boundary','厦门博士后政策和福建省博士后政策有什么区别|厦门博士后和福建博士后有什么区别','厦门博士后政策和福建省博士后政策有什么区别','厦门市政策主要服务在厦设站单位、在厦进站或留厦发展的博士后；福建省政策属于省级口径，覆盖范围、主管层级和申报要求不同。回答厦门问题时应优先厦门口径，省级内容只能作为补充并明确标注。',125)
) v(policy_key, policy_name, slot_code, question_pattern, standard_question, standard_answer, priority)
where not exists (
  select 1 from ai_policy_faq f
  where f.policy_key = v.policy_key
    and f.standard_question = v.standard_question
);

-- 4. intent_phrase：按合理 topic 挂载；如果对应 intent 不存在，该短语不会插入，请看 01 的缺失 intent 检查结果。
with phrase_targets as (
  select *
  from (
    values
    ('double_hundred','双百计划怎么申请','process',null),
    ('double_hundred','双百计划需要什么材料','material',null),
    ('double_hundred','双百计划主管部门是谁','department','route_help'),
    ('xiamen_finance','金融服务产业人才怎么申请','process',null),
    ('xiamen_finance','金融服务产业人才项目需要什么材料','material','process'),
    ('xiamen_finance','CFA 可以申请金融服务产业人才补贴吗','benefit','condition'),
    ('postdoc','博士后补助怎么申请','benefit','process'),
    ('postdoc','博士后工作站怎么申请','station','process'),
    ('postdoc','厦门博士后和福建博士后有什么区别','city_province_boundary','route_help')
  ) t(policy_key, phrase, primary_topic_type, fallback_topic_type)
),
picked as (
  select distinct on (t.policy_key, t.phrase)
         t.policy_key,
         t.phrase,
         i.id intent_id
  from phrase_targets t
  join ai_policy_intent i
    on i.policy_key = t.policy_key
   and i.enabled = true
   and i.topic_type in (t.primary_topic_type, t.fallback_topic_type)
  order by t.policy_key, t.phrase,
           case when i.topic_type = t.primary_topic_type then 0 else 1 end,
           i.priority desc,
           i.id
)
insert into ai_policy_intent_phrase
(base_id, intent_id, phrase, phrase_type, hit_weight, enabled, created_at, updated_at)
select 1, p.intent_id, p.phrase, 'POLICY_STANDARDIZATION_PHASE1', 90, true, now(), now()
from picked p
where not exists (
  select 1 from ai_policy_intent_phrase old
  where old.intent_id = p.intent_id
    and old.phrase = p.phrase
);

-- 5. condition_index：保守补齐。双百计划 policy_no 本轮置 null，避免硬写未经确认文号。
with rows as (
  select *
  from (
    values
    ('double_hundred','厦门市引进高层次创新创业人才“双百计划”实施意见',null,'双百计划,双百,创新创业人才双百计划','identity','innovation_individual','创新个人','创新个人通常通过高层次人才特聘岗位方式遴选，具体以年度通知为准。',120),
    ('double_hundred','厦门市引进高层次创新创业人才“双百计划”实施意见',null,'双百计划,双百,创新创业人才双百计划','identity','innovation_team','创新团队','创新团队通常关注团队构成、项目成果、落地承载和综合评审，具体以年度通知为准。',118),
    ('double_hundred','厦门市引进高层次创新创业人才“双百计划”实施意见',null,'双百计划,双百,创新创业人才双百计划','identity','startup_talent','创业人才','创业人才通常关注创业项目、企业落地、团队和产业贡献，具体以年度通知为准。',118),
    ('double_hundred','厦门市引进高层次创新创业人才“双百计划”实施意见',null,'双百计划,双百,创新创业人才双百计划','enterprise','xiamen_landing','企业落地','用于判断创业项目或承载单位是否与厦门落地相关，不能单独作为符合条件结论。',110),
    ('double_hundred','厦门市引进高层次创新创业人才“双百计划”实施意见',null,'双百计划,双百,创新创业人才双百计划','identity','high_level_talent','高层次人才','用于命中高层次创新创业人才咨询，具体类别和层次以政策文件及年度通知为准。',110),

    ('xiamen_finance','厦门市金融服务产业人才项目实施办法','厦委人才办〔2025〕8号','金融人才,金融服务产业人才,金融服务产业人才项目','industry','finance_org','金融机构','适用于金融机构人才咨询，需结合机构类型、岗位、在厦工作和年度通知判断。',120),
    ('xiamen_finance','厦门市金融服务产业人才项目实施办法','厦委人才办〔2025〕8号','金融人才,金融服务产业人才,金融服务产业人才项目','industry','fund_manager','基金管理机构','适用于基金管理机构人才咨询，具体适用范围以实施办法和年度通知为准。',116),
    ('xiamen_finance','厦门市金融服务产业人才项目实施办法','厦委人才办〔2025〕8号','金融人才,金融服务产业人才,金融服务产业人才项目','industry','local_financial_org','地方金融组织','适用于地方金融组织人才咨询，具体认定口径以主管部门要求为准。',116),
    ('xiamen_finance','厦门市金融服务产业人才项目实施办法','厦委人才办〔2025〕8号','金融人才,金融服务产业人才,金融服务产业人才项目','certificate','cfa','CFA','CFA 可作为金融专业资格相关咨询条件之一，但不能单独判断必然享受补贴。',118),
    ('xiamen_finance','厦门市金融服务产业人才项目实施办法','厦委人才办〔2025〕8号','金融人才,金融服务产业人才,金融服务产业人才项目','certificate','frm','FRM','FRM 可作为金融专业资格相关咨询条件之一，具体补贴以实施办法和年度通知为准。',118),
    ('xiamen_finance','厦门市金融服务产业人才项目实施办法','厦委人才办〔2025〕8号','金融人才,金融服务产业人才,金融服务产业人才项目','certificate','acca','ACCA','ACCA 可作为金融专业资格相关咨询条件之一，具体补贴以实施办法和年度通知为准。',116),
    ('xiamen_finance','厦门市金融服务产业人才项目实施办法','厦委人才办〔2025〕8号','金融人才,金融服务产业人才,金融服务产业人才项目','certificate','fsa','FSA','FSA 可作为金融专业资格相关咨询条件之一，具体补贴以实施办法和年度通知为准。',116),
    ('xiamen_finance','厦门市金融服务产业人才项目实施办法','厦委人才办〔2025〕8号','金融人才,金融服务产业人才,金融服务产业人才项目','education','bachelor','本科','本科条件需结合岗位、机构和年度通知判断，不能单独作为符合条件结论。',105),
    ('xiamen_finance','厦门市金融服务产业人才项目实施办法','厦委人才办〔2025〕8号','金融人才,金融服务产业人才,金融服务产业人才项目','education','doctor_degree','博士','博士学历可作为人才条件参考，需结合其他条件综合判断。',105),
    ('xiamen_finance','厦门市金融服务产业人才项目实施办法','厦委人才办〔2025〕8号','金融人才,金融服务产业人才,金融服务产业人才项目','title','senior_title','高级职称','高级职称可作为人才条件参考，是否符合以实施办法和年度通知为准。',105),

    ('postdoc','关于进一步加强博士后工作的若干措施','厦人社〔2024〕233号','博士后,厦门博士后,博士后工作站,博士后科研工作站,博士后补助','education','doctor_degree','博士','博士学位通常是博士后个人进站的重要前提，仍需结合年龄、设站单位接收和主管流程判断。',120),
    ('postdoc','关于进一步加强博士后工作的若干措施','厦人社〔2024〕233号','博士后,厦门博士后,博士后工作站,博士后科研工作站,博士后补助','identity','postdoctoral','博士后','用于博士后个人、在站补助、出站留厦、工作站等咨询，不自动等同于符合全部补助条件。',120),
    ('postdoc','关于进一步加强博士后工作的若干措施','厦人社〔2024〕233号','博士后,厦门博士后,博士后工作站,博士后科研工作站,博士后补助','platform','postdoc_station','博士后工作站','适用于设站单位、工作站建设和招收博士后咨询，具体设站条件以主管部门要求为准。',116),
    ('postdoc','关于进一步加强博士后工作的若干措施','厦人社〔2024〕233号','博士后,厦门博士后,博士后工作站,博士后科研工作站,博士后补助','process','station_entry','进站','用于博士后个人进站申请咨询，通常需对接设站单位并提交申请材料。',116),
    ('postdoc','关于进一步加强博士后工作的若干措施','厦人社〔2024〕233号','博士后,厦门博士后,博士后工作站,博士后科研工作站,博士后补助','benefit','stay_xiamen','出站留厦','用于博士后出站后留厦就业和安家补贴咨询，具体金额和条件以正式政策为准。',114),
    ('postdoc','关于进一步加强博士后工作的若干措施','厦人社〔2024〕233号','博士后,厦门博士后,博士后工作站,博士后科研工作站,博士后补助','benefit','in_station_subsidy','在站补助','用于博士后在站期间补助咨询，金额、年限和申请要求以正式政策及年度通知为准。',114),
    ('postdoc','关于进一步加强博士后工作的若干措施','厦人社〔2024〕233号','博士后,厦门博士后,博士后工作站,博士后科研工作站,博士后补助','organization','station_unit','设站单位','用于设站单位、接收单位、工作站管理相关咨询，具体以主管部门和单位要求为准。',112)
  ) t(policy_key, policy_name, policy_no, policy_aliases, condition_type, condition_code, condition_text, requirement_text, priority)
)
insert into ai_policy_condition_index
(base_id, policy_name, policy_no, policy_aliases, condition_type, condition_code, condition_text, requirement_text, matched_chunk_id, source_text, priority, status, created_at, updated_at)
select
  1,
  r.policy_name,
  r.policy_no,
  r.policy_aliases,
  r.condition_type,
  r.condition_code,
  r.condition_text,
  r.requirement_text,
  null,
  '[POLICY_STANDARDIZATION_PHASE1] 旧知识库标准化专题卡',
  r.priority,
  'ACTIVE',
  now(),
  now()
from rows r
where not exists (
  select 1 from ai_policy_condition_index c
  where c.policy_name = r.policy_name
    and c.condition_code = r.condition_code
    and coalesce(c.source_text, '') like '%POLICY_STANDARDIZATION_PHASE1%'
);

commit;
