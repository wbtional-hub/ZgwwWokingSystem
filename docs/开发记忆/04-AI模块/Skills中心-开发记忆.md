# Skills中心

## 1. 功能目标

将知识库、提示词、模型、验证题和发布状态组合成可复用的业务技能，并为 AI 工作台提供发布后的技能能力。

## 2. 功能范围

- 技能列表查询
- 技能新增 / 编辑
- 技能版本新增 / 编辑
- 技能版本发布
- 技能绑定知识库
- 技能测试题录入
- 技能验证执行
- 验证详情查看

## 3. 菜单位置

- 后台左侧菜单：`Skills 中心`

## 4. 页面路径

- `/skills`

## 5. 前端文件

- `frontend/src/views/skill/SkillListView.vue`
- `frontend/src/api/skill.js`
- `frontend/src/config/pageHelp.js`

## 6. 后端接口

- `POST /api/skill/list`
- `POST /api/skill/save`
- `POST /api/skill/version/save`
- `POST /api/skill/version/publish`
- `GET /api/skill/{skillId}/published-version`
- `POST /api/skill/binding/save`
- `POST /api/skill/test-case/list`
- `POST /api/skill/test-case/save`
- `POST /api/skill/validation/run`
- `GET /api/skill/validation/{runId}`

## 7. 后端服务/类

- `backend/src/main/java/com/example/lecturesystem/modules/skill/controller/SkillController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/skill/service/SkillService.java`
- `backend/src/main/java/com/example/lecturesystem/modules/skill/service/impl/SkillServiceImpl.java`
- `backend/src/main/resources/mapper/skill/SkillMapper.xml`
- `backend/src/main/resources/mapper/skill/SkillVersionMapper.xml`
- `backend/src/main/resources/mapper/skill/SkillKbBindingMapper.xml`
- `backend/src/main/resources/mapper/skill/SkillTestCaseMapper.xml`
- `backend/src/main/resources/mapper/skill/SkillValidationRunMapper.xml`

## 19. 关联功能

- Skills中心 ↔ 知识库中心 ↔ AI权限配置
- Skills中心 ↔ AI工作台 ↔ 咨询台账 ↔ 月度报表
- Skills中心 ↔ 专家台账 ↔ 个人中心与强制改密
- `backend/src/main/resources/mapper/skill/SkillValidationResultMapper.xml`

## 8. 数据表与关键字段

- `ai_skill`
  - `skill_code`
  - `skill_name`
  - `domain_type`
  - `skill_type`
  - `description`
  - `status`
  - `owner_user_id`
- `ai_skill_version`
  - `skill_id`
  - `version_no`
  - `provider_config_id`
  - `model_code`
  - `system_prompt`
  - `task_prompt`
  - `output_template`
  - `forbidden_rules`
  - `citation_rules`
  - `validation_status`
  - `publish_status`
  - `score`
- `ai_skill_kb_binding`
  - `skill_id`
  - `skill_version_id`
  - `base_id`
  - `category_id`
- `ai_skill_test_case`
  - `case_type`
  - `question_text`
  - `expected_points`
  - `expected_format`
  - `standard_answer`
  - `status`
- `ai_skill_validation_run`
  - `run_status`
  - `pass_rate`
  - `citation_rate`
  - `avg_score`
- `ai_skill_validation_result`
  - 详细字段待补充
- 相关 SQL：
  - `database/20260329_ai_skill_schema.sql`

## 9. 权限规则

- 普通用户能否看到和使用技能，依赖 AI 权限配置与技能授权
- 技能发布后才适合被工作台正式消费
- 技能版本与知识库绑定、验证结果、模型配置存在前置依赖

## 10. 当前状态

已完成首版落地，具备技能、版本、绑定、验证、发布等主链路；但正式记忆此前主要分散在 AI 专项进度文档中。

## 11. 已实现内容

- 技能主数据管理已存在
- 技能版本管理已存在
- 可绑定知识库
- 可录入验证题
- 可执行技能验证并查看验证详情
- 已发布版本可被工作台读取

## 12. 未实现内容

- 更完整的运营流程说明待补充
- 发布前后的验收口径待补充
- 与 AI 接入区、AI 权限配置的专题衔接待补充

## 13. 页面操作步骤
### 第一步：
进入 `/skills`，先查看技能列表，确定是维护已有技能还是新增技能。
### 第二步：
在技能基础信息中维护编码、名称、领域、类型和描述。
### 第三步：
为技能新增或编辑版本，配置模型、系统提示词、任务提示词、输出模板和规则，并绑定目标知识库。
### 第四步：
录入验证题并执行验证，确认结果后发布版本，再供 AI 工作台使用。

## 14. 常见问题

- 技能是否可用，不只看主技能状态，还要看是否存在已发布版本
- 技能验证与正式工作台使用可能引用同一知识库，但目的不同
- 若模型配置或知识库绑定缺失，技能可能无法形成完整闭环

## 15. 风险点

- 技能版本、知识库绑定、验证题、发布状态彼此强耦合
- 已发布版本一旦误被归档或替换，会影响工作台实际可用技能
- 评分、引用率等验证指标若口径回退，容易误导发布判断

## 16. 防回归点

- 验证技能新增、编辑、列表查询主链路可用
- 验证版本保存、发布、已发布版本查询正常
- 验证知识库绑定仍然生效
- 验证验证题录入、验证执行、验证详情查看正常

## 17. 最近一次关键修改

- `2026-03-29`：完成 Skills 中心、技能版本、知识库绑定、技能验证首版落地

## 18. 相关资料与来源文件

- `docs/20260329_ai_overall_design.md`
- `docs/20260329_ai_phase1_spec.md`
- `docs/20260329_ai_api_design.md`
- `docs/20260329_ai_table_definition.md`
- `ai-memory/AI_SKILL_WORKBENCH_PROGRESS_20260329.md`
- `frontend/src/views/skill/SkillListView.vue`
- `frontend/src/api/skill.js`
- `frontend/src/config/pageHelp.js`
- `backend/src/main/resources/mapper/skill/SkillMapper.xml`
- `backend/src/main/resources/mapper/skill/SkillVersionMapper.xml`
- `backend/src/main/resources/mapper/skill/SkillKbBindingMapper.xml`
- `backend/src/main/resources/mapper/skill/SkillTestCaseMapper.xml`
- `backend/src/main/resources/mapper/skill/SkillValidationRunMapper.xml`
